package com.phaithanhcong.user.service;

import com.phaithanhcong.user.model.PaymentOrder;
import com.phaithanhcong.user.model.PaymentStatus;
import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.repository.PaymentOrderRepository;
import com.phaithanhcong.user.repository.PaymentStatusRepository;
import com.phaithanhcong.user.repository.UserRepository;

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
public class PaymentService {

    private static final long PREMIUM_PRICE = 2000L;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private final PayOS payOS;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final UserRepository userRepository;

    public long getPremiumPrice() {
        return PREMIUM_PRICE;
    }

    public CreatePaymentLinkResponse createPayment(User user) throws Exception {
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
    public void markAsPaidManually(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();
        PaymentStatus paid = paymentStatusRepository.findByCode("PAID").get();
        order.setStatus(paid);
        paymentOrderRepository.save(order);

        User user = userRepository.findById(order.getUserId()).get();
        user.setPremium(true);
        userRepository.save(user);
    }
    
    @Transactional
    public void processWebhook(String rawBody) throws Exception {
        var webhookData = payOS.webhooks().verify(rawBody);
        markAsPaidManually(webhookData.getOrderCode());
    }

    public String getStatus(Long orderCode) throws Exception {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();

        if ("PAID".equals(order.getStatus().getCode())) {
            return "PAID";
        }

        var paymentLinkInfo = payOS.paymentRequests().get(orderCode);
        if (paymentLinkInfo.getStatus() == PaymentLinkStatus.PAID) {
            markAsPaidManually(orderCode);
            return "PAID";
        }

        return "PENDING";
    }

    public User getUserIfPaid(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).get();
        return "PAID".equals(order.getStatus().getCode()) ? userRepository.findById(order.getUserId()).get() : null;
    }
}