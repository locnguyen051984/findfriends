package com.phaithanhcong.findfriends.repository;


import com.phaithanhcong.findfriends.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
        Optional<Role> findById(Long id);
        Role findByName(String name);
}
