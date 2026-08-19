package com.phaithanhcong.service.user;

import com.phaithanhcong.model.BrowserTrust;
import com.phaithanhcong.model.User;

import java.util.List;

public interface BrowserTrustService {

    enum BrowserTrustStatus {
        TRUSTED,
        PENDING,
        DENIED
    }

    boolean userCheckOrRegisterBrowser(User user, String browserToken);

    BrowserTrustStatus userGetStatus(User user, String browserToken);

    List<BrowserTrust> userGetPendingRequests(User user);

    void userApprove(Long requestId);

    void userDeny(Long requestId);
}