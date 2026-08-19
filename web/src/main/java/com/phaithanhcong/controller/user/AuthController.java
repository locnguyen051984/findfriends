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

    // ================== LOGIN ==================

    @GetMapping("/")
    public String userLogin() {
        // Trả về file index.html (trang đăng nhập)
        return "index";
    }

   @PostMapping("/login")
   public String handleLogin(@RequestParam String username,
                             @RequestParam String password,
                             HttpSession session,
                             Model model) {
       User user = authService.userLogin(username, password);

       if (user != null) {
           session.setAttribute("loggedInUser", user);
           return "redirect:/home";
       } else {
           model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
           model.addAttribute("username", username);
           return "index";
       }
   }
    // ================== HOME (cần đăng nhập mới vào được) ==================



    // ================== LOGOUT ==================

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Xoá toàn bộ session -> đăng xuất
        return "redirect:/";
    }

    // ================== REGISTER ==================

    @GetMapping("/register")
    public String userRegister() {
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

        boolean success = authService.userRegister(username, password, email, false);

        if (success) {
            model.addAttribute("successMessage", "Đăng ký thành công! Đang chuyển sang trang đăng nhập...");
            return "register";
        } else {
            model.addAttribute("errorMessage", "Đăng ký thất bại! Username đã tồn tại hoặc dữ liệu không hợp lệ.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }
    }

    // ================== FORGOT PASSWORD ==================

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String username,
                                       @RequestParam String email,
                                       Model model) {
        boolean valid = authService.userVerifyAccount(username, email);

        if (valid) {
            return "redirect:/reset-password?username=" + username;
        } else {
            model.addAttribute("errorMessage", "Username hoặc email không khớp với tài khoản nào!");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "forgot-password";
        }
    }

    // ================== RESET PASSWORD ==================

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

        boolean success = authService.userResetPassword(username, newPassword);

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