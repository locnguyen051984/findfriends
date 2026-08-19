package com.phaithanhcong.admin.controller;

import com.phaithanhcong.admin.model.User;
import com.phaithanhcong.admin.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = adminUserService.adminGetAllUsers();
        model.addAttribute("users", users);
        return "users";
    }

    @PostMapping("/toggle-premium")
    public String togglePremium(@RequestParam Long userId) {
        adminUserService.adminTogglePremium(userId);
        return "redirect:/admin/users";
    }
}
