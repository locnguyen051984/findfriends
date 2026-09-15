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

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.SimpUser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.TaskScheduler;

@Slf4j
@RequiredArgsConstructor
@Service
public class CallServiceImpl implements CallService {

    private final CallLogRepository callLogRepository;
    private final CallStatusRepository callStatusRepository;
    private final MessageRepository messageRepository;
    private final MessageTypeRepository messageTypeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final TaskScheduler taskScheduler;


    private final Map<Long, CallState> activeCalls = new ConcurrentHashMap<>();
    private final Map<Long, IceCacheEntry> iceCache = new ConcurrentHashMap<>();

    // ==========================================
    // PUBLIC API (CallService Interface)
    // ==========================================
    
    @jakarta.annotation.PostConstruct
    public void startCleanupJob() {
        taskScheduler.scheduleAtFixedRate(() -> {
            long threshold = System.currentTimeMillis() - 120000; // 2 minutes
            iceCache.entrySet().removeIf(e -> e.getValue().lastActive < threshold);
            activeCalls.entrySet().removeIf(e -> {
                CallState state = e.getValue();
                if (state.lastActive < threshold) {
                    if (state.callLogId != null && state.callLogId != -1L) {
                        boolean ongoing = callLogRepository.findById(state.callLogId)
                                .map(log -> log.getStartedAt() != null && log.getEndedAt() == null)
                                .orElse(false);
                        if (ongoing) {
                            state.lastActive = System.currentTimeMillis();
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            });
        }, Duration.ofMinutes(2));
    }



    @Override
    public void processSignal(CallSignalDTO message, String sessionId) {
        String originalType = message.getType();
        
        // Cache ICE candidates BEFORE handleSignal modifies state
        if ("ICE_CANDIDATE".equals(originalType) && message.getCallLogId() != null) {
            iceCache.computeIfAbsent(message.getCallLogId(), k -> new IceCacheEntry()).candidates.add(message);
        }

        CallSignalDTO result = handleSignal(message, sessionId);

        if ("CALL_BUSY_SERVER".equals(result.getType())) {
            result.setType("CALL_BUSY");
            Long actualCaller = result.getFromUserId();
            result.setFromUserId(result.getToUserId());
            result.setToUserId(actualCaller);
            sendCallSignal(actualCaller, result);
            return;
        }

        if ("OFFER".equals(originalType) && "CALL_FAILED".equals(result.getType())) {
            sendCallSignal(result.getFromUserId(), result);
            return;
        }

        sendCallSignal(result.getToUserId(), result);

        // Forward cached ICE candidates when ANSWER is received
        if ("ANSWER".equals(originalType) && result.getCallLogId() != null) {
            IceCacheEntry cached = iceCache.get(result.getCallLogId());
            if (cached != null) {
                for (CallSignalDTO ice : cached.candidates) {
                    // Only forward ICE candidates from the other user (caller to callee)
                    if (!ice.getFromUserId().equals(result.getFromUserId())) {
                        sendCallSignal(result.getFromUserId(), ice);
                    }
                }
            }
        }

        Optional.ofNullable(buildEchoMessageIfNeeded(result))
                .ifPresent(echo -> sendCallSignal(result.getFromUserId(), echo));

        if (Boolean.TRUE.equals(result.getStateChanged()) && 
            Set.of("CALL_REJECT", "CALL_BUSY", "CALL_END", "CALL_FAILED", "CALL_TIMEOUT").contains(result.getType())) {
            broadcastCallLog(result.getCallLogId());
            iceCache.remove(result.getCallLogId());
        }
    }

    @Override
    public void releaseUserBusyState(Long userId, String disconnectSessionId) {
        if (userId == null) return;
        
        CallState state = activeCalls.get(userId);
        if (state != null && state.activeSessionId != null && disconnectSessionId != null) {
            if (!state.activeSessionId.equals(disconnectSessionId)) {
                return; // Disconnecting session is not the one handling the call
            }
        }

        final String finalSessionId = state != null ? state.activeSessionId : null;
        state = activeCalls.remove(userId);
        Long callLogId = state != null ? state.callLogId : null;
        if (callLogId != null && callLogId != -1L) {
            callLogRepository.findById(callLogId).ifPresent(callLog -> {
                // Allow Callee to navigate pages while ringing (causes brief disconnect) without failing the call
                if (callLog.getStartedAt() == null && callLog.getCalleeId().equals(userId)) {
                    // But if the popup explicitly disconnected and its session matched, we should fail it.
                    // However, we don't know for sure if it was a navigation inside popup or a close.
                    // For safety, let's keep the brief disconnect forgiveness logic if it hasn't started.
                    activeCalls.put(userId, new CallState(callLogId, finalSessionId));
                    return;
                }

                // Notify peer immediately to stop ghost ringing
                Long peerId = callLog.getCallerId().equals(userId) ? callLog.getCalleeId() : callLog.getCallerId();
                CallSignalDTO signal = new CallSignalDTO();
                
                boolean hasStarted = callLog.getStartedAt() != null;
                signal.setType(hasStarted ? "CALL_END" : "CALL_FAILED");
                signal.setFromUserId(userId);
                signal.setToUserId(peerId);
                signal.setCallLogId(callLogId);
                sendCallSignal(peerId, signal);

                String recordStatus = hasStarted ? "COMPLETED" : "FAILED";
                if (userEndCall(callLogId, recordStatus, userId)) {
                    broadcastCallLog(callLogId);
                    iceCache.remove(callLogId);
                }
            });
        }
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private CallSignalDTO handleSignal(CallSignalDTO message, String sessionId) {
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
                    activeCalls.put(fromUserId, new CallState(-1L, sessionId));
                    activeCalls.put(toUserId, new CallState(-1L, null));
                }
                try {
                    CallLog callLog = userStartCall(fromUserId, toUserId, callType);
                    if (callLog != null) {
                        callLogId = callLog.getId();
                        activeCalls.put(fromUserId, new CallState(callLogId, sessionId));
                        activeCalls.put(toUserId, new CallState(callLogId, null));
                        final Long finalCallLogId = callLogId;
                        taskScheduler.schedule(() -> {
                            callLogRepository.findById(finalCallLogId).ifPresent(log -> {
                                if (log.getStartedAt() == null && log.getEndedAt() == null && userEndCall(finalCallLogId, "MISSED", fromUserId)) {
                                    broadcastCallLog(finalCallLogId);
                                }
                            });
                        }, Instant.now().plusSeconds(35));
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
            case "ANSWER" -> {
                CallState calleeState = activeCalls.get(fromUserId);
                if (calleeState != null) {
                    calleeState.activeSessionId = sessionId;
                }
                stateChanged = userMarkStarted(callLogId, fromUserId);
            }
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

    private synchronized boolean userMarkStarted(Long callLogId, Long fromUserId) {
        if (callLogId == null || fromUserId == null) return false;
        return callLogRepository.findById(callLogId).map(callLog -> {
            if (callLog.getEndedAt() != null || callLog.getStartedAt() != null || !isUserInCall(callLog, fromUserId)) return false;
            callLog.setStartedAt(LocalDateTime.now());
            callLogRepository.save(callLog);
            return true;
        }).orElse(false);
    }

    private synchronized boolean userEndCall(Long callLogId, String statusCode, Long fromUserId) {
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
        return "OFFER".equals(result.getType()) || "ANSWER".equals(result.getType()) ? result : null;
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

            sendChatMessage(callLog.getCallerId(), outgoing);
            sendChatMessage(callLog.getCalleeId(), outgoing);
        });
    }

    private void sendCallSignal(Long userId, CallSignalDTO payload) {
        if (simpUserRegistry.getUser(String.valueOf(userId)) == null) {
            log.warn("Cannot send signal to user {} because they are offline", userId);
            return;
        }
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/call", payload);
    }

    private void sendChatMessage(Long userId, Map<String, Object> payload) {
        if (simpUserRegistry.getUser(String.valueOf(userId)) == null) {
            log.warn("Cannot send message to user {} because they are offline", userId);
            return;
        }
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/message", payload);
    }

    private String formatCallDuration(CallLog c) {
        if (c.getStartedAt() == null || c.getEndedAt() == null) return null;
        Duration d = Duration.between(c.getStartedAt(), c.getEndedAt());
        return String.format("%d phút %d giây", d.toMinutes(), d.toSecondsPart());
    }

    private static class CallState {
        final Long callLogId;
        String activeSessionId;
        long lastActive = System.currentTimeMillis();
        CallState(Long id, String sessionId) { this.callLogId = id; this.activeSessionId = sessionId; }
    }

    private static class IceCacheEntry {
        final java.util.List<CallSignalDTO> candidates = new java.util.concurrent.CopyOnWriteArrayList<>();
        final long lastActive = System.currentTimeMillis();
    }
}
