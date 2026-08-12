package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.Admin;
import com.phaithanhcong.findfriends.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminHomeControllerREST {

    private final UserRepository userRepository;

    @GetMapping("/home")
    public ResponseEntity<?> show(HttpSession session) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập"));
        }

        return ResponseEntity.ok(Map.of(
                "adminName", admin.getAdminName(),
                "users", userRepository.findAll()
        ));
    }
}