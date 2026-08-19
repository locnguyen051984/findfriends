package com.phaithanhcong.user.restcontroller;

import com.phaithanhcong.user.dto.UserResponse;
import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthControllerREST {

    private final AuthService authService;

    // ================== LOGIN ==================

    @PostMapping("/login")
    public ResponseEntity<?> userLogin(@RequestParam String username,
                                    @RequestParam String password,
                                    HttpSession session) {
        User user = authService.userLogin(username, password);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        session.setAttribute("loggedInUser", user);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    // ================== LOGOUT ==================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    // ================== REGISTER ==================

    @PostMapping("/register")
    public ResponseEntity<?> userRegister(@RequestParam String username,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       @RequestParam String email) {
        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().build();
        }

        boolean success = authService.userRegister(username, password, email, false);

        if (!success) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    // ================== FORGOT PASSWORD ==================

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String username,
                                             @RequestParam String email) {
        boolean valid = authService.userVerifyAccount(username, email);

        if (!valid) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }

    // ================== RESET PASSWORD ==================

    @PostMapping("/reset-password")
    public ResponseEntity<?> userResetPassword(@RequestParam String username,
                                            @RequestParam String newPassword,
                                            @RequestParam String confirmNewPassword) {
        if (!newPassword.equals(confirmNewPassword)) {
            return ResponseEntity.badRequest().build();
        }

        boolean success = authService.userResetPassword(username, newPassword);

        if (!success) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}
