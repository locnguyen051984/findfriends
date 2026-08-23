package com.phaithanhcong.repository;

import com.phaithanhcong.model.BrowserTrust;
import com.phaithanhcong.model.User;
import com.phaithanhcong.service.user.BrowserTrustService.BrowserTrustStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrowserTrustRepository extends JpaRepository<BrowserTrust, Long> {

    Optional<BrowserTrust> findByUserAndBrowserToken(User user, String browserToken);

    List<BrowserTrust> findByUserAndStatus(User user, BrowserTrustStatus status);

    boolean existsByUserAndStatus(User user, BrowserTrustStatus status);
}