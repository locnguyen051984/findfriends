package com.phaithanhcong.findfriends.service;

import com.phaithanhcong.findfriends.model.BrowserTrust;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.BrowserTrustRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class BrowserTrustService {

    public enum BrowserTrustStatus {
        TRUSTED,
        PENDING,
        DENIED
    }

    private final BrowserTrustRepository browserTrustRepository;

    public boolean checkOrRegisterBrowser(User user, String browserToken) {
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

    public BrowserTrustStatus getStatus(User user, String browserToken) {
        return browserTrustRepository.findByUserAndBrowserToken(user, browserToken)
                .map(BrowserTrust::getStatus)
                .orElse(BrowserTrustStatus.PENDING);
    }

    public List<BrowserTrust> getPendingRequests(User user) {
        return browserTrustRepository.findByUserAndStatus(user, BrowserTrustStatus.PENDING);
    }

    public void approve(Long requestId) {
        browserTrustRepository.findById(requestId).ifPresent(bt -> {
            bt.setStatus(BrowserTrustStatus.TRUSTED);
            browserTrustRepository.save(bt);
        });
    }

    public void deny(Long requestId) {
        browserTrustRepository.findById(requestId).ifPresent(bt -> {
            bt.setStatus(BrowserTrustStatus.DENIED);
            browserTrustRepository.save(bt);
        });
    }
}