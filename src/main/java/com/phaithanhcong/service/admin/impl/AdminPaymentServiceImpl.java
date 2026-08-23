package com.phaithanhcong.service.admin.impl;

import com.phaithanhcong.model.PaymentOrder;
import com.phaithanhcong.repository.PaymentOrderRepository;
import com.phaithanhcong.service.admin.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentOrderRepository paymentOrderRepository;

    @Override
    public List<PaymentOrder> adminGetAllPayments() {
        return paymentOrderRepository.findAll();
    }
}
