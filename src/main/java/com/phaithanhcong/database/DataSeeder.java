package com.phaithanhcong.database;

import com.phaithanhcong.model.User;
import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.MessageType;
import com.phaithanhcong.repository.BrowserTrustRepository;
import com.phaithanhcong.repository.CallStatusRepository;
import com.phaithanhcong.repository.LoginLocationRepository;
import com.phaithanhcong.repository.MessageRepository;
import com.phaithanhcong.repository.MessageTypeRepository;
import com.phaithanhcong.repository.PaymentStatusRepository;
import com.phaithanhcong.repository.UserRepository;
import com.phaithanhcong.model.CallStatus;
import com.phaithanhcong.model.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final MessageRepository messageRepository;
        private final LoginLocationRepository locationRepository;
        private final PaymentStatusRepository paymentStatusRepository;
        private final CallStatusRepository callStatusRepository;
        private final BrowserTrustRepository browserTrustRepository;
        private final MessageTypeRepository messageTypeRepository;

        @Override
        public void run(String... args) {
                seedPaymentStatusIfMissing("PENDING");
                seedPaymentStatusIfMissing("PAID");
                seedCallStatusIfMissing("MISSED");
                seedCallStatusIfMissing("REJECTED");
                seedCallStatusIfMissing("COMPLETED");
                seedCallStatusIfMissing("FAILED");
                seedMessageTypeIfMissing("TEXT");
                seedMessageTypeIfMissing("IMAGE");

                // Clear all existing data in correct dependency order
                browserTrustRepository.deleteAll();
                locationRepository.deleteAll();
                clearUploadedImages();
                messageRepository.deleteAll();
                userRepository.deleteAll();

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
                                .messageType(messageTypeRepository.findByCode("TEXT").orElseThrow())

                                .build());

                messageRepository.save(Message.builder()
                                .senderId(users.get(1).getId())
                                .receiverId(users.get(0).getId())
                                .content("Chào cậu! Mình rảnh đây. Có chuyện gì thế?")
                                .sentAt(LocalDateTime.now().minusMinutes(28))
                                .messageType(messageTypeRepository.findByCode("TEXT").orElseThrow())

                                .build());

                messageRepository.save(Message.builder()
                                .senderId(users.get(0).getId())
                                .receiverId(users.get(1).getId())
                                .content("Mình thấy chúng ta đang ở khá gần nhau trên bản đồ đấy!")
                                .sentAt(LocalDateTime.now().minusMinutes(25))
                                .messageType(messageTypeRepository.findByCode("TEXT").orElseThrow())

                                .build());

                messageRepository.save(Message.builder()
                                .senderId(users.get(1).getId())
                                .receiverId(users.get(0).getId())
                                .content("Ồ thật vậy à? Khoảng bao xa thế cậu?")
                                .sentAt(LocalDateTime.now().minusMinutes(22))
                                .messageType(messageTypeRepository.findByCode("TEXT").orElseThrow())

                                .build());

                messageRepository.save(Message.builder()
                                .senderId(users.get(0).getId())
                                .receiverId(users.get(1).getId())
                                .content("Tầm 1 km hà, bữa nào rảnh ra cà phê giao lưu nha.")
                                .sentAt(LocalDateTime.now().minusMinutes(20))
                                .messageType(messageTypeRepository.findByCode("TEXT").orElseThrow())

                                .build());

                messageRepository.save(Message.builder()
                                .senderId(users.get(0).getId())
                                .receiverId(users.get(2).getId())
                                .content("Hello, mình làm quen nhé!")
                                .sentAt(LocalDateTime.now().minusDays(1))
                                .messageType(messageTypeRepository.findByCode("TEXT").orElseThrow())

                                .build());

                messageRepository.save(Message.builder()
                                .senderId(users.get(2).getId())
                                .receiverId(users.get(0).getId())
                                .content("Chào bạn, rất vui được làm quen.")
                                .sentAt(LocalDateTime.now().minusDays(1).plusMinutes(10))
                                .messageType(messageTypeRepository.findByCode("TEXT").orElseThrow())

                                .build());
        }

        private void seedPaymentStatusIfMissing(String code) {
                if (paymentStatusRepository.findByCode(code).isEmpty()) {
                        paymentStatusRepository.save(PaymentStatus.builder().code(code).build());
                }
        }

        private void seedCallStatusIfMissing(String code) {
                if (callStatusRepository.findByCode(code).isEmpty()) {
                        callStatusRepository.save(CallStatus.builder().code(code).build());
                }
        }

        private void seedMessageTypeIfMissing(String code) {
                if (messageTypeRepository.findByCode(code).isEmpty()) {
                        messageTypeRepository.save(MessageType.builder().code(code).build());
                }
        }

        private void clearUploadedImages() {
                try {
                        Path uploadPath = Paths.get("D:/findfriends-data/uploads/messages/");
                        if (Files.exists(uploadPath)) {
                                Files.list(uploadPath).forEach(file -> {
                                        try {
                                                Files.delete(file);
                                        } catch (IOException ignored) {
                                        }
                                });
                        }
                } catch (IOException e) {
                        throw new RuntimeException("Không xóa được ảnh cũ", e);
                }
        }
}