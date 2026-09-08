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
    private final SimpMessagingTemplate messagingTemplate;

    // Bộ nhớ đệm lưu trạng thái bận (userId -> callLogId)
    private final Map<Long, Long> activeCalls = new java.util.concurrent.ConcurrentHashMap<>();

    public void releaseUserBusyState(Long userId) {
        if (userId != null) {
            activeCalls.remove(userId);
        }
    }

    public CallLog userStartCall(Long callerId, Long calleeId, String callType) {
        if (callerId == null || calleeId == null || callType == null || callType.isEmpty()) {
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

    public Map<String, Object> handleSignal(Map<String, Object> message) {
        String type = String.valueOf(message.get("type"));
        Long fromUserId = Long.valueOf(String.valueOf(message.get("fromUserId")));
        Long toUserId = Long.valueOf(String.valueOf(message.get("toUserId")));
        String callType = message.get("callType") != null ? String.valueOf(message.get("callType")) : "VOICE";
        Long callLogId = message.get("callLogId") != null
                ? Long.valueOf(String.valueOf(message.get("callLogId")))
                : null;

        switch (type) {
            case "OFFER" -> {
                synchronized (activeCalls) {
                    if (activeCalls.containsKey(fromUserId) || activeCalls.containsKey(toUserId)) {
                        message.put("type", "CALL_BUSY_SERVER");
                        return message;
                    }
                    activeCalls.put(fromUserId, -1L);
                    activeCalls.put(toUserId, -1L);
                }

                if (!"VOICE".equals(callType) && !"VIDEO".equals(callType)) {
                    callType = "VOICE";
                }

                try {
                    CallLog callLog = userStartCall(fromUserId, toUserId, callType);
                    callLogId = callLog != null ? callLog.getId() : null;

                    if (callLogId != null) {
                        activeCalls.put(fromUserId, callLogId);
                        activeCalls.put(toUserId, callLogId);
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
            case "ANSWER" -> userMarkStarted(callLogId, fromUserId);
            case "CALL_REJECT" -> userEndCall(callLogId, "REJECTED", fromUserId);
            case "CALL_BUSY" -> userEndCall(callLogId, "REJECTED", fromUserId);
            case "CALL_END" -> userEndCall(callLogId, "COMPLETED", fromUserId);
            case "CALL_FAILED" -> userEndCall(callLogId, "FAILED", fromUserId);
            case "CALL_TIMEOUT" -> userEndCall(callLogId, "MISSED", fromUserId);
            default -> {}
        }

        message.put("callLogId", callLogId);
        return message;
    }

    public Map<String, Object> buildEchoMessageIfNeeded(Map<String, Object> result) {
        if ("OFFER".equals(result.get("type"))) {
            return result;
        }
        return null;
    }

    public void userProcessSignal(Map<String, Object> message) {
        Map<String, Object> result = handleSignal(message);

        if ("CALL_BUSY_SERVER".equals(result.get("type"))) {
            result.put("type", "CALL_BUSY");
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(result.get("fromUserId")),
                    "/queue/call",
                    result
            );
            return;
        }

        messagingTemplate.convertAndSendToUser(
                String.valueOf(result.get("toUserId")),
                "/queue/call",
                result
        );

        Map<String, Object> echo = buildEchoMessageIfNeeded(result);
        if (echo != null) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(result.get("fromUserId")),
                    "/queue/call",
                    echo
            );
        }

        String type = String.valueOf(result.get("type"));
        if ("CALL_REJECT".equals(type) || "CALL_BUSY".equals(type) || "CALL_END".equals(type) || "CALL_FAILED".equals(type) || "CALL_TIMEOUT".equals(type)) {
            Long callLogId = result.get("callLogId") != null
                    ? Long.valueOf(String.valueOf(result.get("callLogId")))
                    : null;
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

        Map<String, Object> outgoing = new HashMap<>();
        outgoing.put("type", "CALL");
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