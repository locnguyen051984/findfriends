package com.phaithanhcong.findfriends.service;

import com.phaithanhcong.findfriends.model.Message;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.MessageRepository;
import com.phaithanhcong.findfriends.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public Message sendMessage(User sender, User receiver, String content) {
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

    public List<Message> getConversation(Long senderId, Long receiverId) {
        return messageRepository
                .findBySenderIdAndReceiverId(senderId, receiverId);
    }
}