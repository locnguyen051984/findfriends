package com.phaithanhcong.controller.user;

import com.phaithanhcong.model.User;
import com.phaithanhcong.service.user.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.util.Map;

@RequiredArgsConstructor
@Controller
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payment")
    public String paymentPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        model.addAttribute("price", paymentService.userGetPremiumPrice());
        return "payment";
    }

    @PostMapping("/payment/create")
    @ResponseBody
    public ResponseEntity<?> userCreatePayment(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }

        try {
            CreatePaymentLinkResponse response = paymentService.userCreatePayment(user);
            session.setAttribute("currentOrderCode", response.getOrderCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi tạo thanh toán: " + e.getMessage());
        }
    }

    @PostMapping("/payment/payos-webhook")
    @ResponseBody
    public ResponseEntity<?> handleWebhook(@RequestBody String rawBody) {
        try {
            paymentService.userProcessWebhook(rawBody);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("Webhook verify failed");
        }
    }

    @GetMapping("/payment/check-status")
    @ResponseBody
    public ResponseEntity<?> checkStatus(HttpSession session) {
        Long orderCode = (Long) session.getAttribute("currentOrderCode");
        if (orderCode == null) {
            return ResponseEntity.badRequest().body("Không có đơn hàng nào");
        }

        try {
            String status = paymentService.userGetStatus(orderCode);

            if ("PAID".equals(status)) {
                User user = paymentService.userGetUserIfPaid(orderCode);
                if (user != null) {
                    session.setAttribute("loggedInUser", user);
                }
            }

            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi kiểm tra trạng thái");
        }
    }

    @GetMapping("/payment/test-mark-paid")
    @ResponseBody
    public ResponseEntity<?> testMarkPaid(@RequestParam Long orderCode) {
        paymentService.userMarkAsPaidManually(orderCode);
        return ResponseEntity.ok("done");
    }
}