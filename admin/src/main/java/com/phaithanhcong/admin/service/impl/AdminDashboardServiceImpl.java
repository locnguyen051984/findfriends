package com.phaithanhcong.admin.service.impl;

import com.phaithanhcong.admin.repository.CallLogRepository;
import com.phaithanhcong.admin.repository.PaymentOrderRepository;
import com.phaithanhcong.admin.repository.UserRepository;
import com.phaithanhcong.admin.service.AdminDashboardService;
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
