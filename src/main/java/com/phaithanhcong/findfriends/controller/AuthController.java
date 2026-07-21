package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.service.AuthService;
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
    public String login() {
        // Trả về file index.html (trang đăng nhập)
        return "index";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              Model model) {
        User user = authService.login(username, password);

        if (user != null) {
            // Đăng nhập thành công -> Chuyển hướng sang trang chủ (ví dụ: /home)
            // Lưu ý: Nếu redirect:/index mà không có @GetMapping("/index") sẽ báo lỗi 404
            return "redirect:/home";
        } else {
            // 1. Đẩy câu thông báo lỗi sang bên Thymeleaf
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");

            // 2. Đẩy lại username để người dùng không phải gõ lại từ đầu
            model.addAttribute("username", username);

            // 3. Trả về lại chính file index.html để hiển thị lỗi
            return "index";
        }
    }

    // Trang đích sau khi đăng nhập thành công
    @GetMapping("/home")
    public String home() {
        return "home"; // Trả về file home.html (bạn cần tạo thêm file này)
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 @RequestParam String email,
                                 Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }

        boolean success = authService.register(username, password, email, false);

        if (success) {
            // Không redirect ngay, trả lại chính register.html kèm successMessage
            // JS trong trang sẽ tự chuyển hướng sau 2 giây
            model.addAttribute("successMessage", "Đăng ký thành công! Đang chuyển sang trang đăng nhập...");
            return "register";
        } else {
            model.addAttribute("errorMessage", "Đăng ký thất bại! Username đã tồn tại hoặc dữ liệu không hợp lệ.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String username,
                                       @RequestParam String email,
                                       Model model) {
        boolean valid = authService.verifyAccount(username, email);

        if (valid) {
            // Verify đúng -> chuyển sang trang đặt password mới, mang theo username
            return "redirect:/reset-password?username=" + username;
        } else {
            model.addAttribute("errorMessage", "Username hoặc email không khớp với tài khoản nào!");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String username, Model model) {
        model.addAttribute("username", username);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String username,
                                      @RequestParam String newPassword,
                                      @RequestParam String confirmNewPassword,
                                      Model model) {
        if (!newPassword.equals(confirmNewPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("username", username);
            return "reset-password";
        }

        boolean success = authService.resetPassword(username, newPassword);

        if (success) {
            model.addAttribute("successMessage", "Đổi mật khẩu thành công! Đang chuyển sang trang đăng nhập...");
            return "reset-password";
        } else {
            model.addAttribute("errorMessage", "Có lỗi xảy ra, vui lòng thử lại!");
            model.addAttribute("username", username);
            return "reset-password";
        }
    }
}