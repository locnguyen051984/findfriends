package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payment")
public class PaymentControllerREST {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            CreatePaymentLinkResponse response = paymentService.createPayment(user);
            session.setAttribute("currentOrderCode", response.getOrderCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi tạo thanh toán: " + e.getMessage()));
        }
    }

    @GetMapping("/check-status")
    public ResponseEntity<?> checkStatus(HttpSession session) {
        Long orderCode = (Long) session.getAttribute("currentOrderCode");
        if (orderCode == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không có đơn hàng nào"));
        }

        try {
            String status = paymentService.getStatus(orderCode);

            if ("PAID".equals(status)) {
                User user = paymentService.getUserIfPaid(orderCode);
                if (user != null) {
                    session.setAttribute("loggedInUser", user);
                }
            }

            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi kiểm tra trạng thái"));
        }
    }

    @GetMapping("/test-mark-paid")
    public ResponseEntity<?> testMarkPaid(@RequestParam Long orderCode) {
        paymentService.markAsPaidManually(orderCode);
        return ResponseEntity.ok(Map.of("message", "done"));
    }

    // Giữ nguyên path cũ vì đây là URL đã cấu hình trên PayOS dashboard
    @PostMapping("/payment/payos-webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String rawBody) {
        try {
            paymentService.processWebhook(rawBody);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("message", "Webhook verify failed"));
        }
    }
}