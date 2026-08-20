package com.phaithanhcong.findfriends.model;

import com.phaithanhcong.findfriends.service.BrowserTrustService.BrowserTrustStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "browser_trust")
public class BrowserTrust {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "browser_token", nullable = false)
    private String browserToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BrowserTrustStatus status;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;
}