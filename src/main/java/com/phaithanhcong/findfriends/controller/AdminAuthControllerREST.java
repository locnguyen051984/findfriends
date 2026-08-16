package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.dto.AdminResponse;
import com.phaithanhcong.findfriends.model.Admin;
import com.phaithanhcong.findfriends.service.AdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        session.setAttribute("loggedInAdmin", admin);
        return ResponseEntity.ok(AdminResponse.fromEntity(admin));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }
}