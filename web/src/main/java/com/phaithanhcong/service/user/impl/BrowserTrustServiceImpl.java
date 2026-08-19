package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.BrowserTrustService;

import com.phaithanhcong.model.BrowserTrust;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.BrowserTrustRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class BrowserTrustServiceImpl implements BrowserTrustService {

    private final BrowserTrustRepository browserTrustRepository;

    public boolean userCheckOrRegisterBrowser(User user, String browserToken) {
        Optional<BrowserTrust> existing = browserTrustRepository.findByUserAndBrowserToken(user, browserToken);

        if (existing.isPresent() && BrowserTrustStatus.TRUSTED.equals(existing.get().getStatus())) {
            return true;
        }

        boolean hasAnyTrusted = browserTrustRepository.existsByUserAndStatus(user, BrowserTrustStatus.TRUSTED);

        BrowserTrust record = existing.orElse(
                BrowserTrust.builder().user(user).browserToken(browserToken).build());
        record.setCreatedAt(LocalDateTime.now());

        if (!hasAnyTrusted) {
            record.setStatus(BrowserTrustStatus.TRUSTED);
            browserTrustRepository.save(record);
            return true;
        }

        record.setStatus(BrowserTrustStatus.PENDING);
        browserTrustRepository.save(record);
        return false;
    }

    public BrowserTrustStatus userGetStatus(User user, String browserToken) {
        return browserTrustRepository.findByUserAndBrowserToken(user, browserToken)
                .map(BrowserTrust::getStatus)
                .orElse(BrowserTrustStatus.PENDING);
    }

    public List<BrowserTrust> userGetPendingRequests(User user) {
        return browserTrustRepository.findByUserAndStatus(user, BrowserTrustStatus.PENDING);
    }

    public void userApprove(Long requestId) {
        browserTrustRepository.findById(requestId).ifPresent(bt -> {
            bt.setStatus(BrowserTrustStatus.TRUSTED);
            browserTrustRepository.save(bt);
        });
    }

    public void userDeny(Long requestId) {
        browserTrustRepository.findById(requestId).ifPresent(bt -> {
            bt.setStatus(BrowserTrustStatus.DENIED);
            browserTrustRepository.save(bt);
        });
    }
}