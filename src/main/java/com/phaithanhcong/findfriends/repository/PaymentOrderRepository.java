package com.phaithanhcong.findfriends.repository;

import com.phaithanhcong.findfriends.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderCode(Long orderCode);
}