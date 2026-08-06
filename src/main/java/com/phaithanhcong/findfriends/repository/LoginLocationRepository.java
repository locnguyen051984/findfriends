package com.phaithanhcong.findfriends.repository;

import com.phaithanhcong.findfriends.model.LoginLocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginLocationRepository extends JpaRepository<LoginLocation, Long> {

    List<LoginLocation> findByUserIdOrderByLoginAtDesc(Long userId, Pageable pageable);
}