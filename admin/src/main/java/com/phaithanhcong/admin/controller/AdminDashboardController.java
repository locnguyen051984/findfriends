package com.phaithanhcong.admin.controller;

import com.phaithanhcong.admin.repository.UserRepository;
import com.phaithanhcong.admin.repository.PaymentOrderRepository;
import com.phaithanhcong.admin.repository.CallLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PaymentOrderRepository paymentOrderRepository;
    
    @Autowired
    private CallLogRepository callLogRepository;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        long totalUsers = userRepository.count();
        long premiumUsers = userRepository.countByPremiumTrue();
        long totalPayments = paymentOrderRepository.count();
        long totalCalls = callLogRepository.count();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("premiumUsers", premiumUsers);
        model.addAttribute("totalPayments", totalPayments);
        model.addAttribute("totalCalls", totalCalls);
        
        return "dashboard";
    }
}
