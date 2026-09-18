package com.phaithanhcong.controller.admin;

import com.phaithanhcong.model.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    @GetMapping({"", "/"})
    public String dashboard(
            HttpSession session,
            Model model) {

        User admin =
                (User) session.getAttribute("loggedInUser");

        // Chưa đăng nhập
        if (admin == null) {
            return "redirect:/";
        }

        // Không phải ADMIN
        if (admin.getRole() == null
                || !"ADMIN".equalsIgnoreCase(
                admin.getRole().trim())) {

            return "redirect:/home";
        }

        model.addAttribute("admin", admin);

        return "admin/dashboard";
    }
}