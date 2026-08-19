package com.phaithanhcong.admin.controller;

import com.phaithanhcong.admin.model.PaymentOrder;
import com.phaithanhcong.admin.repository.PaymentOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @GetMapping
    public String listPayments(Model model) {
        List<PaymentOrder> payments = paymentOrderRepository.findAll();
        model.addAttribute("payments", payments);
        return "payments";
    }
}
