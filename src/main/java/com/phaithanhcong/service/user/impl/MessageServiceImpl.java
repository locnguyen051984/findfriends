package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.MessageService;

import com.phaithanhcong.model.CallLog;
import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.CallLogRepository;
import com.phaithanhcong.repository.MessageRepository;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor


public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CallLogRepository callLogRepository;

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
            item.put("content", m.getContent());
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
}