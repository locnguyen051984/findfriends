package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.Admin;
import com.phaithanhcong.findfriends.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequiredArgsConstructor
@Controller
public class AdminHomeController {

    private final UserRepository userRepository;

    @GetMapping("/admin-home")
    public String show(HttpSession session, Model model) {
        Admin admin = (Admin) session.getAttribute("loggedInAdmin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("adminName", admin.getAdminName());
        model.addAttribute("users", userRepository.findAll());
        return "admin-home";
    }
}