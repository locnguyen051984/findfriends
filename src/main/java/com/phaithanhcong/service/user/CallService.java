package com.phaithanhcong.service.user;

import com.phaithanhcong.model.CallLog;
import java.util.Map;

public interface CallService {
    CallLog userStartCall(Long callerId, Long calleeId, String callType);
    boolean userMarkStarted(Long callLogId, Long fromUserId);
    boolean userEndCall(Long callLogId, String statusCode, Long fromUserId);
    Map<String, Object> handleSignal(Map<String, Object> message);
    Map<String, Object> buildEchoMessageIfNeeded(Map<String, Object> result);
    void userProcessSignal(Map<String, Object> message);
    void releaseUserBusyState(Long userId);
}
