package com.phaithanhcong.findfriends.service;

import com.phaithanhcong.findfriends.model.PaymentOrder;
import com.phaithanhcong.findfriends.model.PaymentStatus;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.PaymentOrderRepository;
import com.phaithanhcong.findfriends.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private static final long PREMIUM_PRICE = 50000L;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private final PayOS payOS;
    private final PaymentOrderRepository paymentOrderRepository;
    private final UserRepository userRepository;

    public CreatePaymentLinkResponse createPayment(User user) throws Exception {
        Long orderCode = System.currentTimeMillis() / 1000;

        PaymentOrder order = PaymentOrder.builder()
                .orderCode(orderCode)
                .userId(user.getId())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        paymentOrderRepository.save(order);

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(PREMIUM_PRICE)
                .description("Nang cap Premium")
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        return payOS.paymentRequests().create(paymentData);
    }

    public void processWebhook(String rawBody) throws Exception {
        var webhookData = payOS.webhooks().verify(rawBody);

        PaymentOrder order = paymentOrderRepository.findByOrderCode(webhookData.getOrderCode()).get();
        order.setStatus(PaymentStatus.PAID);
        paymentOrderRepository.save(order);

        User user = userRepository.findById(order.getUserId()).get();
        user.setPremium(true);
        userRepository.save(user);
    }

    public PaymentStatus getStatus(Long orderCode) {
        return paymentOrderRepository.findByOrderCode(orderCode).get().getStatus();
    }

    public User getUserIfPaid(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();
        return order.getStatus() == PaymentStatus.PAID ? userRepository.findById(order.getUserId()).get() : null;
    }
    public void markAsPaidManually(Long orderCode) {
    PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();
    order.setStatus(PaymentStatus.PAID);
    paymentOrderRepository.save(order);

    User user = userRepository.findById(order.getUserId()).get();
    user.setPremium(true);
    userRepository.save(user);
    }

    
}