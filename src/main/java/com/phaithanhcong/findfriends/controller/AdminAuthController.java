package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.Admin;
import com.phaithanhcong.findfriends.service.AdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    private final AdminService adminService;

    @GetMapping("/login")
    public String login() {
        return "admin-login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {
        Admin admin = adminService.login(username, password);

        if (admin != null) {
            session.setAttribute("loggedInAdmin", admin);
            return "redirect:/admin-home";
        } else {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu Admin không chính xác!");
            model.addAttribute("username", username);
            return "admin-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }
}
