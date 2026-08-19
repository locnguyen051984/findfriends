package com.phaithanhcong.service.admin;

public interface AdminDashboardService {
    long adminGetTotalUsers();
    long adminGetPremiumUsers();
    long adminGetTotalPayments();
    long adminGetTotalCalls();
}
