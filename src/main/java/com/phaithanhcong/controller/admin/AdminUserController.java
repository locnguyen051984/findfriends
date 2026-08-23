package com.phaithanhcong.controller.admin;

import com.phaithanhcong.model.User;
import com.phaithanhcong.service.admin.AdminUserService;
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
        return "admin/users";
    }

    @PostMapping("/toggle-premium")
    public String togglePremium(@RequestParam Long userId) {
        adminUserService.adminTogglePremium(userId);
        return "redirect:/admin/users";
    }
}
