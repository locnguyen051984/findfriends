package com.phaithanhcong.service.admin.impl;

import com.phaithanhcong.repository.CallLogRepository;
import com.phaithanhcong.repository.PaymentOrderRepository;
import com.phaithanhcong.repository.UserRepository;
import com.phaithanhcong.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final CallLogRepository callLogRepository;

    @Override
    public long adminGetTotalUsers() {
        return userRepository.count();
    }

    @Override
    public long adminGetPremiumUsers() {
        return userRepository.countByPremiumTrue();
    }

    @Override
    public long adminGetTotalPayments() {
        return paymentOrderRepository.count();
    }

    @Override
    public long adminGetTotalCalls() {
        return callLogRepository.count();
    }
}
