package com.phaithanhcong.service.user;

import com.phaithanhcong.model.CallLog;
import com.phaithanhcong.dto.CallSignalDTO;

public interface CallService {
    CallLog userStartCall(Long callerId, Long calleeId, String callType);
    boolean userMarkStarted(Long callLogId, Long fromUserId);
    boolean userEndCall(Long callLogId, String statusCode, Long fromUserId);
    CallSignalDTO handleSignal(CallSignalDTO message);
    CallSignalDTO buildEchoMessageIfNeeded(CallSignalDTO result);
    void userProcessSignal(CallSignalDTO message);
    void releaseUserBusyState(Long userId);
}
