package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.Message;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.UserRepository;
import com.phaithanhcong.findfriends.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;


    @GetMapping("/{otherUserId}")
    public String showConversation(
            @PathVariable Long otherUserId,
            HttpSession session,
            Model model) {

        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/"; // chưa login thì đá về trang login
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        List<Message> conversation = messageService.getConversation(currentUser.getId(), otherUser.getId());

        model.addAttribute("conversation", conversation);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);

        return "message"; // -> templates/message.html
    }

    /*@PostMapping("/{otherUserId}")
    public String sendMessage(
            @PathVariable Long otherUserId,
            @RequestParam String content,
            HttpSession session,
            Model model) {

        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/";
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        try {
            messageService.sendMessage(currentUser, otherUser, content);
            return "redirect:/messages/" + otherUserId;
        } catch (RuntimeException e) {
            List<Message> conversation = messageService.getConversation(currentUser.getId(), otherUser.getId());
            model.addAttribute("conversation", conversation);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("otherUser", otherUser);
            model.addAttribute("error", e.getMessage());
            return "message";
        }
    }

    // Lấy trực tiếp từ session, không cần query lại DB
    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}*/
@MessageMapping("/chat.send")
    public void sendMessage(Map<String, Object> payload) {
        Long senderId = Long.valueOf(payload.get("senderId").toString());
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = payload.get("content").toString();

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender không tồn tại"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver không tồn tại"));

        Message saved = messageService.sendMessage(sender, receiver, content);

        String topic = "/topic/messages/" + conversationId(sender.getId(), receiver.getId());
        messagingTemplate.convertAndSend(topic, saved);
    }

    private String conversationId(Long id1, Long id2) {
        long min = Math.min(id1, id2);
        long max = Math.max(id1, id2);
        return min + "-" + max;
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}