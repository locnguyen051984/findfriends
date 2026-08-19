package com.phaithanhcong.admin.service;

public interface AdminDashboardService {
    long adminGetTotalUsers();
    long adminGetPremiumUsers();
    long adminGetTotalPayments();
    long adminGetTotalCalls();
}
