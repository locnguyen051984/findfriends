package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class HomeController {

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            // Chưa đăng nhập mà cố vào /home -> đá về trang login
            return "redirect:/";
        }
        if (user.getRole() == null) {
            // User tồn tại nhưng không có role -> không cho vào trang chủ
            session.invalidate();
            return "redirect:/";
        }
        model.addAttribute("username", user.getUserName());
        return "home";
    }
}
