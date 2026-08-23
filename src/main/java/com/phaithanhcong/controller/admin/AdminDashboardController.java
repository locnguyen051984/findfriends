package com.phaithanhcong.controller.admin;

import com.phaithanhcong.service.admin.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        long totalUsers = adminDashboardService.adminGetTotalUsers();
        long premiumUsers = adminDashboardService.adminGetPremiumUsers();
        long totalPayments = adminDashboardService.adminGetTotalPayments();
        long totalCalls = adminDashboardService.adminGetTotalCalls();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("premiumUsers", premiumUsers);
        model.addAttribute("totalPayments", totalPayments);
        model.addAttribute("totalCalls", totalCalls);
        
        return "admin/dashboard";
    }
}
