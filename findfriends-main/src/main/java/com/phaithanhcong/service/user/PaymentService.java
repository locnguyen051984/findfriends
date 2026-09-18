package com.phaithanhcong.service.user;

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

public interface PaymentService {
    long userGetPremiumPrice();
    CreatePaymentLinkResponse userCreatePayment(User user) throws Exception;
    void userMarkAsPaidManually(Long orderCode);
    void userProcessWebhook(String rawBody) throws Exception;
    String userGetStatus(Long orderCode) throws Exception;
    User userGetUserIfPaid(Long orderCode);
}
