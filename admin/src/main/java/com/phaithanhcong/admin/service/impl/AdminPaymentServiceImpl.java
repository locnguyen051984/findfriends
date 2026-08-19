package com.phaithanhcong.admin.service.impl;

import com.phaithanhcong.admin.model.PaymentOrder;
import com.phaithanhcong.admin.repository.PaymentOrderRepository;
import com.phaithanhcong.admin.service.AdminPaymentService;
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
