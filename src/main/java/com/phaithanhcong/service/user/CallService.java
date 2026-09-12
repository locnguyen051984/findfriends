package com.phaithanhcong.service.user;

import com.phaithanhcong.dto.CallSignalDTO;

public interface CallService {
    void processSignal(CallSignalDTO message);
    void releaseUserBusyState(Long userId);
}
