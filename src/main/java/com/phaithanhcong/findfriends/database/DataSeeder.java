package com.phaithanhcong.findfriends.database;

import com.phaithanhcong.findfriends.model.Admin;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.model.Message;
import com.phaithanhcong.findfriends.model.PaymentStatus;
import com.phaithanhcong.findfriends.repository.AdminRepository;
import com.phaithanhcong.findfriends.repository.LoginLocationRepository;
import com.phaithanhcong.findfriends.repository.MessageRepository;
import com.phaithanhcong.findfriends.repository.PaymentStatusRepository;
import com.phaithanhcong.findfriends.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final MessageRepository messageRepository;
    private final LoginLocationRepository locationRepository;
    private final PaymentStatusRepository paymentStatusRepository;

    @Override
    public void run(String... args) {
        seedPaymentStatusIfMissing("PENDING");
        seedPaymentStatusIfMissing("PAID");

        // Clear all existing data in correct dependency order
        locationRepository.deleteAll();
        messageRepository.deleteAll();
        userRepository.deleteAll();
        adminRepository.deleteAll();

        // 1. Seed Admin
        Admin admin = Admin.builder()
                .adminName("admin")
                .password("1")
                .email("admin@gmail.com")
                .build();
        adminRepository.save(admin);

        // 2. Seed Users (test1 to test9)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            User user = User.builder()
                    .userName("test" + i)
                    .password("1")
                    .email("test" + i + "@gmail.com")
                    .premium(i % 2 == 0)
                    .build();
            users.add(userRepository.save(user));
        }

        // 3. Seed Messages 
        messageRepository.save(Message.builder()
                .senderId(users.get(0).getId())
                .receiverId(users.get(1).getId())
                .content("Chào cậu, cậu có rảnh không?")
                .sentAt(LocalDateTime.now().minusMinutes(30))
                .build());

        messageRepository.save(Message.builder()
                .senderId(users.get(1).getId())
                .receiverId(users.get(0).getId())
                .content("Chào cậu! Mình rảnh đây. Có chuyện gì thế?")
                .sentAt(LocalDateTime.now().minusMinutes(28))
                .build());

        messageRepository.save(Message.builder()
                .senderId(users.get(0).getId())
                .receiverId(users.get(1).getId())
                .content("Mình thấy chúng ta đang ở khá gần nhau trên bản đồ đấy!")
                .sentAt(LocalDateTime.now().minusMinutes(25))
                .build());

        messageRepository.save(Message.builder()
                .senderId(users.get(1).getId())
                .receiverId(users.get(0).getId())
                .content("Ồ thật vậy à? Khoảng bao xa thế cậu?")
                .sentAt(LocalDateTime.now().minusMinutes(22))
                .build());

        messageRepository.save(Message.builder()
                .senderId(users.get(0).getId())
                .receiverId(users.get(1).getId())
                .content("Tầm 1 km hà, bữa nào rảnh ra cà phê giao lưu nha.")
                .sentAt(LocalDateTime.now().minusMinutes(20))
                .build());

        messageRepository.save(Message.builder()
                .senderId(users.get(0).getId())
                .receiverId(users.get(2).getId())
                .content("Hello, mình làm quen nhé!")
                .sentAt(LocalDateTime.now().minusDays(1))
                .build());

        messageRepository.save(Message.builder()
                .senderId(users.get(2).getId())
                .receiverId(users.get(0).getId())
                .content("Chào bạn, rất vui được làm quen.")
                .sentAt(LocalDateTime.now().minusDays(1).plusMinutes(10))
                .build());
    }

    private void seedPaymentStatusIfMissing(String code) {
        if (paymentStatusRepository.findByCode(code).isEmpty()) {
            paymentStatusRepository.save(PaymentStatus.builder().code(code).build());
        }
    }
}