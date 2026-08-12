package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.Admin;
import com.phaithanhcong.findfriends.service.AdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminAuthControllerREST {

    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                    @RequestParam String password,
                                    HttpSession session) {
        Admin admin = adminService.login(username, password);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Tên đăng nhập hoặc mật khẩu Admin không chính xác!"));
        }

        session.setAttribute("loggedInAdmin", admin);
        return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công", "admin", admin));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }
}