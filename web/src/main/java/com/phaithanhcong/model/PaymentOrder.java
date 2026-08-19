package com.phaithanhcong.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payment_orders")
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}