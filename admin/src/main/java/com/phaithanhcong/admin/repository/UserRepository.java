package com.phaithanhcong.admin.repository;

import com.phaithanhcong.admin.model.User;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);
    
    long countByPremiumTrue();

    List<User> findAllByEmail(String email);

    Optional<User> findByUserNameAndEmail(String userName, String email);

    Optional<User> findById(Long id);
}
