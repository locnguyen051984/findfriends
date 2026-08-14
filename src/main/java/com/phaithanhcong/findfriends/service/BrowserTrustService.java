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

    private final BrowserTrustRepository browserTrustRepository;

    public static final String TRUSTED = "TRUSTED";
    public static final String PENDING = "PENDING";
    public static final String DENIED = "DENIED";

    public boolean checkOrRegisterBrowser(User user, String browserToken) {
        Optional<BrowserTrust> existing = browserTrustRepository.findByUserAndBrowserToken(user, browserToken);

        if (existing.isPresent() && TRUSTED.equals(existing.get().getStatus())) {
            return true;
        }

        boolean hasAnyTrusted = browserTrustRepository.existsByUserAndStatus(user, TRUSTED);

        BrowserTrust record = existing.orElse(
                BrowserTrust.builder().user(user).browserToken(browserToken).build());
        record.setCreatedAt(LocalDateTime.now());

        if (!hasAnyTrusted) {
            record.setStatus(TRUSTED);
            browserTrustRepository.save(record);
            return true;
        }

        record.setStatus(PENDING);
        browserTrustRepository.save(record);
        return false;
    }

    public String getStatus(User user, String browserToken) {
        return browserTrustRepository.findByUserAndBrowserToken(user, browserToken)
                .map(BrowserTrust::getStatus)
                .orElse(PENDING);
    }

    public List<BrowserTrust> getPendingRequests(User user) {
        return browserTrustRepository.findByUserAndStatus(user, PENDING);
    }

    public void approve(Long requestId) {
        browserTrustRepository.findById(requestId).ifPresent(bt -> {
            bt.setStatus(TRUSTED);
            browserTrustRepository.save(bt);
        });
    }

    public void deny(Long requestId) {
        browserTrustRepository.findById(requestId).ifPresent(bt -> {
            bt.setStatus(DENIED);
            browserTrustRepository.save(bt);
        });
    }
}