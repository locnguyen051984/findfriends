package com.phaithanhcong.findfriends.service;

import com.phaithanhcong.findfriends.model.Admin;
import com.phaithanhcong.findfriends.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final AdminRepository adminRepository;

    public Admin login(String adminName, String password) {
        if (adminName == null || adminName.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }

        Admin admin = adminRepository.findByAdminName(adminName).orElse(null);
        if (admin == null) {
            return null;
        }

        if (admin.getPassword().equals(password)) {
            return admin;
        }
        return null;
    }
}
