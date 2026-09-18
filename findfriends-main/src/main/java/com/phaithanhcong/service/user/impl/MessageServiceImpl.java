package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.MessageService;

import com.phaithanhcong.model.CallLog;
import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.MessageType;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.CallLogRepository;
import com.phaithanhcong.repository.MessageRepository;
import com.phaithanhcong.repository.MessageTypeRepository;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CallLogRepository callLogRepository;
    private final MessageTypeRepository messageTypeRepository;
    private static final String UPLOAD_DIR = "D:/findfriends-data/uploads/messages/";
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    public Message userSendMessage(User sender, User receiver, String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Nội dung tin nhắn không được để trống");
        }

        User sender1 = userRepository.findById(sender.getId())
                .orElseThrow(() -> new RuntimeException("Sender không tồn tại"));
        User receiver1 = userRepository.findById(receiver.getId())
                .orElseThrow(() -> new RuntimeException("Receiver không tồn tại"));

        Message message = Message.builder()
                .senderId(sender1.getId())
                .receiverId(receiver1.getId())
                .content(content)
                .sentAt(LocalDateTime.now())
                .messageType(getTypeOrThrow("TEXT"))
                .build();

        return messageRepository.save(message);
    }

    public List<Message> userGetConversation(Long senderId, Long receiverId) {
        return messageRepository
                .findBySenderIdAndReceiverId(senderId, receiverId);
    }

    // Gộp tin nhắn + lịch sử cuộc gọi thành 1 timeline, sort theo thời gian
    public List<Map<String, Object>> userBuildTimeline(User currentUser, User otherUser) {
        List<Message> messages = userGetConversation(currentUser.getId(), otherUser.getId());
        List<CallLog> calls = callLogRepository.findByCallerIdAndCalleeIdOrCalleeIdAndCallerId(
                currentUser.getId(), otherUser.getId(), currentUser.getId(), otherUser.getId());

        List<Map<String, Object>> timeline = new ArrayList<>();

        for (Message m : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "MESSAGE");
            item.put("time", m.getSentAt());
            item.put("isMe", m.getSenderId().equals(currentUser.getId()));
            item.put("messageType", m.getMessageType() != null ? m.getMessageType().getCode() : "TEXT");
            item.put("content", m.getContent());
            item.put("imageUrl", m.getImageUrl());
            timeline.add(item);
        }

        for (CallLog c : calls) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "CALL");
            item.put("time", c.getCreatedAt());
            item.put("isMe", c.getCallerId().equals(currentUser.getId()));
            item.put("callType", c.getCallType()); // VOICE | VIDEO
            String statusCode = c.getStatus() != null ? c.getStatus().getCode() : "MISSED";
            item.put("statusCode", statusCode);
            item.put("durationText", formatCallDuration(c));
            timeline.add(item);
        }

        timeline.sort(Comparator.comparing(item -> (LocalDateTime) item.get("time")));
        return timeline;
    }

    private String formatCallDuration(CallLog c) {
        if (c.getStartedAt() == null || c.getEndedAt() == null) {
            return null; // MISSED / REJECTED -> không có thời lượng
        }
        Duration d = Duration.between(c.getStartedAt(), c.getEndedAt());
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();
        return minutes + " phút " + seconds + " giây";
    }

    public Message userSendImage(User sender, User receiver, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File ảnh không được để trống");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new RuntimeException("Ảnh vượt quá 5MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Chỉ hỗ trợ JPG, PNG, WEBP");
        }

        User sender1 = userRepository.findById(sender.getId())
                .orElseThrow(() -> new RuntimeException("Sender không tồn tại"));
        User receiver1 = userRepository.findById(receiver.getId())
                .orElseThrow(() -> new RuntimeException("Receiver không tồn tại"));

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String ext = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            Message message = Message.builder()
                    .senderId(sender1.getId())
                    .receiverId(receiver1.getId())
                    .messageType(getTypeOrThrow("IMAGE"))
                    .imageUrl("/uploads/messages/" + fileName)
                    .sentAt(LocalDateTime.now())
                    .build();

            return messageRepository.save(message);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu file ảnh", e);
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains("."))
            return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    private MessageType getTypeOrThrow(String code) {
        return messageTypeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("MessageType không tồn tại: " + code));
    }
}