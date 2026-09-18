package com.phaithanhcong.repository;

import com.phaithanhcong.model.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageTypeRepository extends JpaRepository<MessageType, Long> {
    Optional<MessageType> findByCode(String code);
}