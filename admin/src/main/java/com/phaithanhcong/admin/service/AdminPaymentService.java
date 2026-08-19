package com.phaithanhcong.admin.service;

import com.phaithanhcong.admin.model.PaymentOrder;
import java.util.List;

public interface AdminPaymentService {
    List<PaymentOrder> adminGetAllPayments();
}
