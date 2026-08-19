package com.phaithanhcong.service.admin;

import com.phaithanhcong.model.PaymentOrder;
import java.util.List;

public interface AdminPaymentService {
    List<PaymentOrder> adminGetAllPayments();
}
