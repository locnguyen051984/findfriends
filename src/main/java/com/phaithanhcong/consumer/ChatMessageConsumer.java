package com.phaithanhcong.consumer;

import com.phaithanhcong.config.RabbitConfig;
import com.phaithanhcong.dto.ChatMessageTask;
import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import com.phaithanhcong.service.user.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitConfig.CHAT_QUEUE, concurrency = "1")
    public void consume(ChatMessageTask task) {
        User sender = userRepository.findById(task.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender không tồn tại: " + task.getSenderId()));
        User receiver = userRepository.findById(task.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver không tồn tại: " + task.getReceiverId()));

        Message saved = messageService.userSendMessage(sender, receiver, task.getContent());

        Map<String, Object> outgoing = new HashMap<>();
        outgoing.put("type", "MESSAGE");
        outgoing.put("senderId", saved.getSenderId());
        outgoing.put("receiverId", saved.getReceiverId());
        outgoing.put("content", saved.getContent());
        outgoing.put("sentAt", saved.getSentAt().toString());

        messagingTemplate.convertAndSendToUser(String.valueOf(task.getReceiverId()), "/queue/message", outgoing);
        messagingTemplate.convertAndSendToUser(String.valueOf(task.getSenderId()), "/queue/message", outgoing);
    }
}
