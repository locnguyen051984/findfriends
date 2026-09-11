package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.CallService;

import com.phaithanhcong.model.CallLog;
import com.phaithanhcong.model.CallStatus;
import com.phaithanhcong.repository.CallLogRepository;
import com.phaithanhcong.repository.CallStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service


public class CallServiceImpl implements CallService {

    private final CallLogRepository callLogRepository;
    private final CallStatusRepository callStatusRepository;
    private final com.phaithanhcong.repository.MessageRepository messageRepository;
    private final com.phaithanhcong.repository.MessageTypeRepository messageTypeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Bộ nhớ đệm lưu trạng thái bận (userId -> callLogId)
    private final Map<Long, Long> activeCalls = new java.util.concurrent.ConcurrentHashMap<>();
    
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(10);

    public void releaseUserBusyState(Long userId) {
        if (userId != null) {
            Long callLogId = activeCalls.get(userId);
            if (callLogId != null && callLogId != -1L) {
                boolean ended = userEndCall(callLogId, "FAILED", userId);
                if (ended) {
                    broadcastCallLog(callLogId);
                }
            }
            activeCalls.remove(userId);
        }
    }

    public CallLog userStartCall(Long callerId, Long calleeId, String callType) {
        if (callerId == null || calleeId == null || callType == null || callType.isEmpty()) {
            return null;
        }
        if (callerId.equals(calleeId)) {
            return null;
        }

        CallLog callLog = CallLog.builder()
                .callerId(callerId)
                .calleeId(calleeId)
                .callType(callType)
                .createdAt(LocalDateTime.now())
                .build();

        return callLogRepository.save(callLog);
    }

    public boolean userMarkStarted(Long callLogId, Long fromUserId) {
        if (callLogId == null || fromUserId == null) {
            return false;
        }
        CallLog callLog = callLogRepository.findById(callLogId).orElse(null);
        if (callLog == null) {
            return false;
        }

        if (callLog.getEndedAt() != null || callLog.getStartedAt() != null) {
            return false;
        }

        if (!callLog.getCallerId().equals(fromUserId) && !callLog.getCalleeId().equals(fromUserId)) {
            return false;
        }

        callLog.setStartedAt(LocalDateTime.now());
        callLogRepository.save(callLog);
        return true;
    }

    public boolean userEndCall(Long callLogId, String statusCode, Long fromUserId) {
        if (callLogId == null || statusCode == null || statusCode.isEmpty() || fromUserId == null) {
            return false;
        }
        CallLog callLog = callLogRepository.findById(callLogId).orElse(null);
        if (callLog == null) {
            return false;
        }

        if (callLog.getEndedAt() != null) {
            return false;
        }

        if (!callLog.getCallerId().equals(fromUserId) && !callLog.getCalleeId().equals(fromUserId)) {
            return false;
        }

        if ("COMPLETED".equals(statusCode) && callLog.getStartedAt() == null) {
            statusCode = "MISSED";
        }

        CallStatus status = callStatusRepository.findByCode(statusCode).orElse(null);
        if (status == null) {
            return false;
        }
        callLog.setStatus(status);
        callLog.setEndedAt(LocalDateTime.now());
        callLogRepository.save(callLog);

        activeCalls.remove(callLog.getCallerId());
        activeCalls.remove(callLog.getCalleeId());

        return true;
    }

    public com.phaithanhcong.dto.CallSignalDTO handleSignal(com.phaithanhcong.dto.CallSignalDTO message) {
        String type = message.getType();
        Long fromUserId = message.getFromUserId();
        Long toUserId = message.getToUserId();
        String callType = message.getCallType() != null ? message.getCallType() : "VOICE";
        Long callLogId = message.getCallLogId();

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
                    callLogId = callLog != null ? callLog.getId() : null;

                    if (callLogId != null) {
                        activeCalls.put(fromUserId, callLogId);
                        activeCalls.put(toUserId, callLogId);

                        final Long finalCallLogId = callLogId;
                        scheduler.schedule(() -> {
                            CallLog log = callLogRepository.findById(finalCallLogId).orElse(null);
                            if (log != null && log.getStartedAt() == null && log.getEndedAt() == null) {
                                boolean ended = userEndCall(finalCallLogId, "MISSED", fromUserId);
                                if (ended) {
                                    broadcastCallLog(finalCallLogId);
                                }
                            }
                        }, 35, java.util.concurrent.TimeUnit.SECONDS);

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
                boolean ok = userMarkStarted(callLogId, fromUserId);
                message.setStateChanged(ok);
            }
            case "CALL_REJECT" -> {
                boolean ok = userEndCall(callLogId, "REJECTED", fromUserId);
                message.setStateChanged(ok);
            }
            case "CALL_BUSY" -> {
                boolean ok = userEndCall(callLogId, "REJECTED", fromUserId);
                message.setStateChanged(ok);
            }
            case "CALL_END" -> {
                boolean ok = userEndCall(callLogId, "COMPLETED", fromUserId);
                message.setStateChanged(ok);
            }
            case "CALL_FAILED" -> {
                boolean ok = userEndCall(callLogId, "FAILED", fromUserId);
                message.setStateChanged(ok);
            }
            case "CALL_TIMEOUT" -> {
                boolean ok = userEndCall(callLogId, "MISSED", fromUserId);
                message.setStateChanged(ok);
            }
            default -> {}
        }

        message.setCallLogId(callLogId);
        return message;
    }

    public com.phaithanhcong.dto.CallSignalDTO buildEchoMessageIfNeeded(com.phaithanhcong.dto.CallSignalDTO result) {
        if ("OFFER".equals(result.getType())) {
            return result;
        }
        return null;
    }

    public void userProcessSignal(com.phaithanhcong.dto.CallSignalDTO message) {
        com.phaithanhcong.dto.CallSignalDTO result = handleSignal(message);

        if ("CALL_BUSY_SERVER".equals(result.getType())) {
            result.setType("CALL_BUSY");
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(result.getFromUserId()),
                    "/queue/call",
                    result
            );
            return;
        }

        messagingTemplate.convertAndSendToUser(
                String.valueOf(result.getToUserId()),
                "/queue/call",
                result
        );

        com.phaithanhcong.dto.CallSignalDTO echo = buildEchoMessageIfNeeded(result);
        if (echo != null) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(result.getFromUserId()),
                    "/queue/call",
                    echo
            );
        }

        String type = result.getType();
        boolean stateChanged = Boolean.TRUE.equals(result.getStateChanged());
        if (("CALL_REJECT".equals(type) || "CALL_BUSY".equals(type) || "CALL_END".equals(type) || "CALL_FAILED".equals(type) || "CALL_TIMEOUT".equals(type)) && stateChanged) {
            Long callLogId = result.getCallLogId();
            broadcastCallLog(callLogId);
        }
    }

    // Gửi bản ghi call_log đã hoàn tất về timeline chat (queue/message) cho cả caller và callee
    private void broadcastCallLog(Long callLogId) {
        if (callLogId == null) {
            return;
        }
        CallLog callLog = callLogRepository.findById(callLogId).orElse(null);
        if (callLog == null) {
            return;
        }

        com.phaithanhcong.model.MessageType typeCall = messageTypeRepository.findByCode("CALL").orElseGet(() -> {
            com.phaithanhcong.model.MessageType t = com.phaithanhcong.model.MessageType.builder().code("CALL").build();
            return messageTypeRepository.save(t);
        });

        com.phaithanhcong.model.Message msg = com.phaithanhcong.model.Message.builder()
                .messageType(typeCall)
                .callLogId(callLog.getId())
                .senderId(callLog.getCallerId())
                .receiverId(callLog.getCalleeId())
                .sentAt(LocalDateTime.now())
                .content(callLog.getCallType() + " call")
                .build();
        messageRepository.save(msg);

        Map<String, Object> outgoing = new HashMap<>();
        outgoing.put("type", "CALL");
        outgoing.put("callLogId", callLogId);
        outgoing.put("callerId", callLog.getCallerId());
        outgoing.put("calleeId", callLog.getCalleeId());
        outgoing.put("callType", callLog.getCallType());
        outgoing.put("statusCode", callLog.getStatus() != null ? callLog.getStatus().getCode() : "MISSED");
        outgoing.put("time", callLog.getCreatedAt().toString());
        outgoing.put("durationText", formatCallDuration(callLog));

        messagingTemplate.convertAndSendToUser(String.valueOf(callLog.getCallerId()), "/queue/message", outgoing);
        messagingTemplate.convertAndSendToUser(String.valueOf(callLog.getCalleeId()), "/queue/message", outgoing);
    }

    private String formatCallDuration(CallLog c) {
        if (c.getStartedAt() == null || c.getEndedAt() == null) {
            return null;
        }
        Duration d = Duration.between(c.getStartedAt(), c.getEndedAt());
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return minutes + " phút " + seconds + " giây";
    }
}