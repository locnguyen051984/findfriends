package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.dto.CallSignalDTO;
import com.phaithanhcong.model.CallLog;
import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.MessageType;
import com.phaithanhcong.repository.CallLogRepository;
import com.phaithanhcong.repository.CallStatusRepository;
import com.phaithanhcong.repository.MessageRepository;
import com.phaithanhcong.repository.MessageTypeRepository;
import com.phaithanhcong.service.user.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class CallServiceImpl implements CallService {

    private final CallLogRepository callLogRepository;
    private final CallStatusRepository callStatusRepository;
    private final MessageRepository messageRepository;
    private final MessageTypeRepository messageTypeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<Long, Long> activeCalls = new ConcurrentHashMap<>();
    private final Map<Long, java.util.List<CallSignalDTO>> iceCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // ==========================================
    // PUBLIC API (CallService Interface)
    // ==========================================

    @Override
    public void processSignal(CallSignalDTO message) {
        String originalType = message.getType();
        
        // Cache ICE candidates BEFORE handleSignal modifies state
        if ("ICE_CANDIDATE".equals(originalType) && message.getCallLogId() != null) {
            iceCache.computeIfAbsent(message.getCallLogId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(message);
        }

        CallSignalDTO result = handleSignal(message);

        if ("CALL_BUSY_SERVER".equals(result.getType())) {
            result.setType("CALL_BUSY");
            Long actualCaller = result.getFromUserId();
            result.setFromUserId(result.getToUserId());
            result.setToUserId(actualCaller);
            sendToUser(actualCaller, result);
            return;
        }

        if ("OFFER".equals(originalType) && "CALL_FAILED".equals(result.getType())) {
            sendToUser(result.getFromUserId(), result);
            return;
        }

        sendToUser(result.getToUserId(), result);

        // Forward cached ICE candidates when ANSWER is received
        if ("ANSWER".equals(originalType) && result.getCallLogId() != null) {
            java.util.List<CallSignalDTO> cached = iceCache.get(result.getCallLogId());
            if (cached != null) {
                for (CallSignalDTO ice : cached) {
                    // Only forward ICE candidates from the other user (caller to callee)
                    if (!ice.getFromUserId().equals(result.getFromUserId())) {
                        sendToUser(result.getFromUserId(), ice);
                    }
                }
            }
        }

        Optional.ofNullable(buildEchoMessageIfNeeded(result))
                .ifPresent(echo -> sendToUser(result.getFromUserId(), echo));

        if (Boolean.TRUE.equals(result.getStateChanged()) && 
            Set.of("CALL_REJECT", "CALL_BUSY", "CALL_END", "CALL_FAILED", "CALL_TIMEOUT").contains(result.getType())) {
            broadcastCallLog(result.getCallLogId());
            iceCache.remove(result.getCallLogId());
        }
    }

    @Override
    public void releaseUserBusyState(Long userId) {
        if (userId == null) return;
        Long callLogId = activeCalls.remove(userId);
        if (callLogId != null && callLogId != -1L) {
            callLogRepository.findById(callLogId).ifPresent(callLog -> {
                // Allow Callee to navigate pages while ringing (causes brief disconnect) without failing the call
                if (callLog.getStartedAt() == null && callLog.getCalleeId().equals(userId)) {
                    activeCalls.put(userId, callLogId);
                    return;
                }

                // Notify peer immediately to stop ghost ringing
                Long peerId = callLog.getCallerId().equals(userId) ? callLog.getCalleeId() : callLog.getCallerId();
                CallSignalDTO failedSignal = new CallSignalDTO();
                failedSignal.setType("CALL_FAILED");
                failedSignal.setFromUserId(userId);
                failedSignal.setToUserId(peerId);
                failedSignal.setCallLogId(callLogId);
                sendToUser(peerId, failedSignal);

                if (userEndCall(callLogId, "FAILED", userId)) {
                    broadcastCallLog(callLogId);
                    iceCache.remove(callLogId);
                }
            });
        }
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private CallSignalDTO handleSignal(CallSignalDTO message) {
        Long fromUserId = message.getFromUserId(), toUserId = message.getToUserId(), callLogId = message.getCallLogId();
        String type = message.getType(), callType = message.getCallType();
        boolean stateChanged = false;

        switch (type) {
            case "OFFER" -> {
                if (!"VOICE".equals(callType) && !"VIDEO".equals(callType)) {
                    message.setType("CALL_FAILED");
                    return message;
                }
                synchronized (activeCalls) {
                    if (activeCalls.containsKey(fromUserId) || activeCalls.containsKey(toUserId)) {
                        message.setType("CALL_BUSY_SERVER");
                        return message;
                    }
                    activeCalls.put(fromUserId, -1L);
                    activeCalls.put(toUserId, -1L);
                }
                try {
                    CallLog callLog = userStartCall(fromUserId, toUserId, callType);
                    if (callLog != null) {
                        callLogId = callLog.getId();
                        activeCalls.put(fromUserId, callLogId);
                        activeCalls.put(toUserId, callLogId);
                        final Long finalCallLogId = callLogId;
                        scheduler.schedule(() -> {
                            callLogRepository.findById(finalCallLogId).ifPresent(log -> {
                                if (log.getStartedAt() == null && log.getEndedAt() == null && userEndCall(finalCallLogId, "MISSED", fromUserId)) {
                                    broadcastCallLog(finalCallLogId);
                                }
                            });
                        }, 35, TimeUnit.SECONDS);
                    } else {
                        activeCalls.remove(fromUserId);
                        activeCalls.remove(toUserId);
                    }
                } catch (Exception e) {
                    activeCalls.remove(fromUserId);
                    activeCalls.remove(toUserId);
                    throw e;
                }
            }
            case "ANSWER" -> stateChanged = userMarkStarted(callLogId, fromUserId);
            case "CALL_REJECT", "CALL_BUSY" -> stateChanged = userEndCall(callLogId, "REJECTED", fromUserId);
            case "CALL_END" -> stateChanged = userEndCall(callLogId, "COMPLETED", fromUserId);
            case "CALL_FAILED" -> stateChanged = userEndCall(callLogId, "FAILED", fromUserId);
            case "CALL_TIMEOUT" -> stateChanged = userEndCall(callLogId, "MISSED", fromUserId);
        }

        message.setStateChanged(stateChanged);
        message.setCallLogId(callLogId);
        return message;
    }

    private CallLog userStartCall(Long callerId, Long calleeId, String callType) {
        if (callerId == null || calleeId == null || callType == null || callerId.equals(calleeId)) return null;
        return callLogRepository.save(CallLog.builder()
                .callerId(callerId)
                .calleeId(calleeId)
                .callType(callType)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private boolean userMarkStarted(Long callLogId, Long fromUserId) {
        if (callLogId == null || fromUserId == null) return false;
        return callLogRepository.findById(callLogId).map(callLog -> {
            if (callLog.getEndedAt() != null || callLog.getStartedAt() != null || !isUserInCall(callLog, fromUserId)) return false;
            callLog.setStartedAt(LocalDateTime.now());
            callLogRepository.save(callLog);
            return true;
        }).orElse(false);
    }

    private boolean userEndCall(Long callLogId, String statusCode, Long fromUserId) {
        if (callLogId == null || statusCode == null || fromUserId == null) return false;
        return callLogRepository.findById(callLogId).map(callLog -> {
            if (callLog.getEndedAt() != null || !isUserInCall(callLog, fromUserId)) return false;
            
            String finalStatus = ("COMPLETED".equals(statusCode) && callLog.getStartedAt() == null) ? "MISSED" : statusCode;
            return callStatusRepository.findByCode(finalStatus).map(status -> {
                callLog.setStatus(status);
                callLog.setEndedAt(LocalDateTime.now());
                callLogRepository.save(callLog);
                activeCalls.remove(callLog.getCallerId());
                activeCalls.remove(callLog.getCalleeId());
                return true;
            }).orElse(false);
        }).orElse(false);
    }

    private boolean isUserInCall(CallLog callLog, Long userId) {
        return callLog.getCallerId().equals(userId) || callLog.getCalleeId().equals(userId);
    }

    private CallSignalDTO buildEchoMessageIfNeeded(CallSignalDTO result) {
        return "OFFER".equals(result.getType()) ? result : null;
    }

    private void broadcastCallLog(Long callLogId) {
        if (callLogId == null) return;
        callLogRepository.findById(callLogId).ifPresent(callLog -> {
            MessageType typeCall = messageTypeRepository.findByCode("CALL")
                    .orElseGet(() -> messageTypeRepository.save(MessageType.builder().code("CALL").build()));

            messageRepository.save(Message.builder()
                    .messageType(typeCall)
                    .callLogId(callLog.getId())
                    .senderId(callLog.getCallerId())
                    .receiverId(callLog.getCalleeId())
                    .sentAt(LocalDateTime.now())
                    .content(callLog.getCallType() + " call")
                    .build());

            Map<String, Object> outgoing = new HashMap<>();
            outgoing.put("type", "CALL");
            outgoing.put("callLogId", callLogId);
            outgoing.put("callerId", callLog.getCallerId());
            outgoing.put("calleeId", callLog.getCalleeId());
            outgoing.put("callType", callLog.getCallType());
            outgoing.put("statusCode", callLog.getStatus() != null ? callLog.getStatus().getCode() : "MISSED");
            outgoing.put("time", callLog.getCreatedAt().toString());
            outgoing.put("durationText", formatCallDuration(callLog));

            sendToUser(callLog.getCallerId(), outgoing);
            sendToUser(callLog.getCalleeId(), outgoing);
        });
    }

    private void sendToUser(Long userId, CallSignalDTO payload) {
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/call", payload);
    }

    private void sendToUser(Long userId, Map<String, Object> payload) {
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/message", payload);
    }

    private String formatCallDuration(CallLog c) {
        if (c.getStartedAt() == null || c.getEndedAt() == null) return null;
        Duration d = Duration.between(c.getStartedAt(), c.getEndedAt());
        return String.format("%d phút %d giây", d.toMinutes(), d.toSecondsPart());
    }
}