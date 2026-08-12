package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthControllerREST {

    private final AuthService authService;

    // ================== LOGIN ==================

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                    @RequestParam String password,
                                    HttpSession session) {
        User user = authService.login(username, password);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không chính xác!"));
        }

        session.setAttribute("loggedInUser", user);
        return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công", "user", user));
    }

    // ================== LOGOUT ==================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    // ================== REGISTER ==================

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       @RequestParam String email) {
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mật khẩu xác nhận không khớp!"));
        }

        boolean success = authService.register(username, password, email, false);

        if (!success) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Đăng ký thất bại! Username đã tồn tại hoặc dữ liệu không hợp lệ."));
        }

        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
    }

    // ================== FORGOT PASSWORD ==================

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String username,
                                             @RequestParam String email) {
        boolean valid = authService.verifyAccount(username, email);

        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username hoặc email không khớp với tài khoản nào!"));
        }

        return ResponseEntity.ok(Map.of("message", "Xác thực thành công", "username", username));
    }

    // ================== RESET PASSWORD ==================

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String username,
                                            @RequestParam String newPassword,
                                            @RequestParam String confirmNewPassword) {
        if (!newPassword.equals(confirmNewPassword)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mật khẩu xác nhận không khớp!"));
        }

        boolean success = authService.resetPassword(username, newPassword);

        if (!success) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Có lỗi xảy ra, vui lòng thử lại!"));
        }

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
}