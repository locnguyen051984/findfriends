package com.phaithanhcong.user.restcontroller;

import com.phaithanhcong.user.dto.PaymentStatusResponse;
import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/payment")
public class PaymentControllerREST {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> userCreatePayment(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            CreatePaymentLinkResponse response = paymentService.userCreatePayment(user);
            session.setAttribute("currentOrderCode", response.getOrderCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/check-status")
    public ResponseEntity<?> checkStatus(HttpSession session) {
        Long orderCode = (Long) session.getAttribute("currentOrderCode");
        if (orderCode == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String status = paymentService.userGetStatus(orderCode);

            if ("PAID".equals(status)) {
                User user = paymentService.userGetUserIfPaid(orderCode);
                if (user != null) {
                    session.setAttribute("loggedInUser", user);
                }
            }

            return ResponseEntity.ok(new PaymentStatusResponse(status));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/test-mark-paid")
    public ResponseEntity<?> testMarkPaid(@RequestParam Long orderCode) {
        paymentService.userMarkAsPaidManually(orderCode);
        return ResponseEntity.ok().build();
    }

    // Giữ nguyên path cũ vì đây là URL đã cấu hình trên PayOS dashboard
    @PostMapping("/payment/payos-webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String rawBody) {
        try {
            paymentService.userProcessWebhook(rawBody);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).build();
        }
    }
}
