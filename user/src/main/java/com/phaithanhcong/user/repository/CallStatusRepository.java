package com.phaithanhcong.user.repository;

import com.phaithanhcong.user.model.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CallStatusRepository extends JpaRepository<CallStatus, Long> {
    Optional<CallStatus> findByCode(String code);
}