package com.phaithanhcong.user.service.impl;

import com.phaithanhcong.user.service.CallService;

import com.phaithanhcong.user.model.CallLog;
import com.phaithanhcong.user.model.CallStatus;
import com.phaithanhcong.user.repository.CallLogRepository;
import com.phaithanhcong.user.repository.CallStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
@Service


public class CallServiceImpl implements CallService {

    private final CallLogRepository callLogRepository;
    private final CallStatusRepository callStatusRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Tạo bản ghi call_log khi caller bắt đầu gọi (lúc gửi OFFER)
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

    // Cập nhật khi callee accept -> bắt đầu tính giờ call
    public boolean userMarkStarted(Long callLogId) {
        if (callLogId == null) {
            return false;
        }
        CallLog callLog = callLogRepository.findById(callLogId).orElse(null);
        if (callLog == null) {
            return false;
        }
        callLog.setStartedAt(LocalDateTime.now());
        callLogRepository.save(callLog);
        return true;
    }

    // Kết thúc call, gán status tương ứng (COMPLETED, MISSED, REJECTED, FAILED)
    public boolean userEndCall(Long callLogId, String statusCode) {
        if (callLogId == null || statusCode == null || statusCode.isEmpty()) {
            return false;
        }
        CallLog callLog = callLogRepository.findById(callLogId).orElse(null);
        if (callLog == null) {
            return false;
        }

        // Nếu cuộc gọi kết thúc nhưng chưa từng kết nối (startedAt == null) -> Coi như cuộc gọi nhỡ (MISSED)
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
        return true;
    }

    // Xử lý logic ghi/update call_log dựa theo loại signal, trả về callLogId (nếu có) để gắn lại vào message
    public Map<String, Object> handleSignal(Map<String, Object> message) {
        String type = String.valueOf(message.get("type"));
        Long fromUserId = Long.valueOf(String.valueOf(message.get("fromUserId")));
        Long toUserId = Long.valueOf(String.valueOf(message.get("toUserId")));
        String callType = String.valueOf(message.get("callType"));
        Long callLogId = message.get("callLogId") != null
                ? Long.valueOf(String.valueOf(message.get("callLogId")))
                : null;

        switch (type) {
            case "OFFER" -> {
                CallLog callLog = userStartCall(fromUserId, toUserId, callType);
                callLogId = callLog != null ? callLog.getId() : null;
            }
            case "ANSWER" -> userMarkStarted(callLogId);
            case "CALL_REJECT" -> userEndCall(callLogId, "REJECTED");
            case "CALL_END" -> userEndCall(callLogId, "COMPLETED");
            default -> {
            } // ICE_CANDIDATE: không đụng call_log
        }

        message.put("callLogId", callLogId);
        return message;
    }

    // Xác định message nào cần gửi thêm (echo callLogId về cho chính caller khi OFFER)
    // Trả về null nếu không cần gửi thêm.
    public Map<String, Object> buildEchoMessageIfNeeded(Map<String, Object> result) {
        if ("OFFER".equals(result.get("type"))) {
            return result;
        }
        return null;
    }

    // Gộp toàn bộ flow: xử lý signal + gửi tin qua WebSocket cho target và echo cho sender nếu cần
    public void userProcessSignal(Map<String, Object> message) {
        Map<String, Object> result = handleSignal(message);

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
    }
}