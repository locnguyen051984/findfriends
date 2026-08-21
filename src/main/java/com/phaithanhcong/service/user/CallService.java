package com.phaithanhcong.service.user;

import com.phaithanhcong.model.CallLog;
import com.phaithanhcong.model.CallStatus;
import com.phaithanhcong.repository.CallLogRepository;
import com.phaithanhcong.repository.CallStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;

public interface CallService {
    CallLog userStartCall(Long callerId, Long calleeId, String callType);
    boolean userMarkStarted(Long callLogId);
    boolean userEndCall(Long callLogId, String statusCode);
    Map<String, Object> handleSignal(Map<String, Object> message);
    Map<String, Object> buildEchoMessageIfNeeded(Map<String, Object> result);
    void userProcessSignal(Map<String, Object> message);
}
