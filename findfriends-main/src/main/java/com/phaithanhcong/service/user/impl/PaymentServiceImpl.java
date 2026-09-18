package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.PaymentService;

import com.phaithanhcong.model.PaymentOrder;
import com.phaithanhcong.model.PaymentStatus;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.PaymentOrderRepository;
import com.phaithanhcong.repository.PaymentStatusRepository;
import com.phaithanhcong.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service


public class PaymentServiceImpl implements PaymentService {

    private static final long PREMIUM_PRICE = 2000L;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private final PayOS payOS;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final UserRepository userRepository;

    public long userGetPremiumPrice() {
        return PREMIUM_PRICE;
    }

    public CreatePaymentLinkResponse userCreatePayment(User user) throws Exception {
        Long orderCode = System.currentTimeMillis() / 1000;

        PaymentStatus pending = paymentStatusRepository.findByCode("PENDING").get();

        PaymentOrder order = PaymentOrder.builder()
                .orderCode(orderCode)
                .userId(user.getId())
                .status(pending)
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

    @Transactional
    public void userMarkAsPaidManually(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();
        PaymentStatus paid = paymentStatusRepository.findByCode("PAID").get();
        order.setStatus(paid);
        paymentOrderRepository.save(order);

        User user = userRepository.findById(order.getUserId()).get();
        user.setPremium(true);
        userRepository.save(user);
    }
    
    @Transactional
    public void userProcessWebhook(String rawBody) throws Exception {
        var webhookData = payOS.webhooks().verify(rawBody);
        userMarkAsPaidManually(webhookData.getOrderCode());
    }

    public String userGetStatus(Long orderCode) throws Exception {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();

        if ("PAID".equals(order.getStatus().getCode())) {
            return "PAID";
        }

        var paymentLinkInfo = payOS.paymentRequests().get(orderCode);
        if (paymentLinkInfo.getStatus() == PaymentLinkStatus.PAID) {
            userMarkAsPaidManually(orderCode);
            return "PAID";
        }

        return "PENDING";
    }

    public User userGetUserIfPaid(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();
        return "PAID".equals(order.getStatus().getCode()) ? userRepository.findById(order.getUserId()).get() : null;
    }
}