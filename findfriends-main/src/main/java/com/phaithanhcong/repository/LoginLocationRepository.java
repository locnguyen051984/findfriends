package com.phaithanhcong.repository;

import com.phaithanhcong.model.LoginLocation;
import com.phaithanhcong.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoginLocationRepository extends JpaRepository<LoginLocation, Long> {

    List<LoginLocation> findByUserOrderByLoginAtDesc(User user);

    Optional<LoginLocation> findFirstByUserOrderByLoginAtDesc(User user);
}