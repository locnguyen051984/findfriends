package com.phaithanhcong.findfriends.repository;

import com.phaithanhcong.findfriends.model.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CallStatusRepository extends JpaRepository<CallStatus, Long> {
    Optional<CallStatus> findByCode(String code);
}