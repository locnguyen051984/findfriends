package com.phaithanhcong.controller.user;

import com.phaithanhcong.model.User;
import com.phaithanhcong.service.user.AuthService;

import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
public class AuthController {

    private final AuthService authService;
    @GetMapping("/")
    public String userLogin() {

        return "index";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // Tìm tài khoản trong Database
        User user =
                authService.userLogin(
                        username,
                        password
                );
        // ĐĂNG NHẬP THẤT BẠI
        if (user == null) {

            model.addAttribute(
                    "errorMessage",
                    "Tên đăng nhập hoặc mật khẩu không chính xác!"
            );
            model.addAttribute(
                    "username",
                    username
            );
            return "index";
        }
        // ĐĂNG NHẬP THÀNH CÔNG
        session.setAttribute(
                "loggedInUser",
                user
        );
        // KIỂM TRA ROLE
        String role = user.getRole();
        // ADMIN -> TRANG ADMIN
        if (role != null
                && "ADMIN".equalsIgnoreCase(
                role.trim())) {
            return "redirect:/admin";
        }
        // USER -> TRANG USER
        return "redirect:/home";
    }
    // LOGOUT
    @GetMapping("/logout")
    public String logout(
            HttpSession session) {

        session.invalidate();

        return "redirect:/";
    }
    // REGISTER PAGE
    @GetMapping("/register")
    public String userRegister() {

        return "register";
    }
    // REGISTER
    @PostMapping("/register")
    public String handleRegister(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String email,
            Model model) {

        // Kiểm tra mật khẩu
        if (!password.equals(confirmPassword)) {
            model.addAttribute(
                    "errorMessage",
                    "Mật khẩu xác nhận không khớp!"
            );
            model.addAttribute(
                    "username",
                    username
            );
            model.addAttribute(
                    "email",
                    email
            );
            return "register";
        }
        // Đăng ký tài khoản USER
        boolean success =
                authService.userRegister(
                        username,
                        password,
                        email,
                        false
                );
        if (success) {
            model.addAttribute(
                    "successMessage",
                    "Đăng ký thành công! Đang chuyển sang trang đăng nhập..."
            );
            return "register";
        } else {
            model.addAttribute(
                    "errorMessage",
                    "Đăng ký thất bại! Username đã tồn tại hoặc dữ liệu không hợp lệ."
            );
            model.addAttribute(
                    "username",
                    username
            );
            model.addAttribute(
                    "email",
                    email
            );
            return "register";
        }
    }
    // FORGOT PASSWORD
    @GetMapping("/forgot-password")
    public String forgotPassword() {

        return "forgot-password";
    }
    // FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam String username,
            @RequestParam String email,
            Model model) {
        String token =
                authService.createResetToken(
                        username,
                        email
                );
        if (token != null) {
            return "redirect:/reset-password?token="
                    + token;
        } else {
            model.addAttribute(
                    "errorMessage",
                    "Username hoặc email không khớp với tài khoản nào!"
            );
            model.addAttribute(
                    "username",
                    username
            );
            model.addAttribute(
                    "email",
                    email
            );
            return "forgot-password";
        }
    }
    // RESET PASSWORD PAGE
    @GetMapping("/reset-password")
    public String resetPasswordForm(
            @RequestParam String token,
            Model model) {
        if (!authService.isValidResetToken(token)) {
            model.addAttribute(
                    "errorMessage",
                    "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn!"
            );
            return "forgot-password";
        }
        model.addAttribute(
                "token",
                token
        );
        return "reset-password";
    }
    // RESET PASSWORD
    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmNewPassword,
            Model model) {

        // Kiểm tra mật khẩu
        if (!newPassword.equals(
                confirmNewPassword)) {
            model.addAttribute(
                    "errorMessage",
                    "Mật khẩu xác nhận không khớp!"
            );
            model.addAttribute(
                    "token",
                    token
            );
            return "reset-password";
        }
        boolean success =
                authService.resetPasswordByToken(
                        token,
                        newPassword
                );
        if (success) {

            model.addAttribute(
                    "successMessage",
                    "Đổi mật khẩu thành công! Đang chuyển sang trang đăng nhập..."
            );
            return "reset-password";
        } else {
            model.addAttribute(
                    "errorMessage",
                    "Token không hợp lệ, đã hết hạn hoặc mật khẩu không hợp lệ!"
            );
            model.addAttribute(
                    "token",
                    token
            );
            return "reset-password";
        }
    }
}