package com.phaithanhcong.findfriends.repository;

import com.phaithanhcong.findfriends.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByAdminName(String adminName);
}
