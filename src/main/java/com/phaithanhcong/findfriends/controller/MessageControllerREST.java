package com.phaithanhcong.findfriends.controller;

import com.phaithanhcong.findfriends.model.Message;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.UserRepository;
import com.phaithanhcong.findfriends.service.MessageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageControllerREST {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @GetMapping("/{otherUserId}")
    public ResponseEntity<?> showConversation(@PathVariable Long otherUserId, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập"));
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        List<Message> conversation = messageService.getConversation(currentUser.getId(), otherUser.getId());

        return ResponseEntity.ok(Map.of(
                "conversation", conversation,
                "currentUser", currentUser,
                "otherUser", otherUser
        ));
    }

    @PostMapping("/{otherUserId}")
    public ResponseEntity<?> sendMessage(@PathVariable Long otherUserId,
                                          @RequestParam String content,
                                          HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Chưa đăng nhập"));
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        try {
            messageService.sendMessage(currentUser, otherUser, content);
            List<Message> conversation = messageService.getConversation(currentUser.getId(), otherUser.getId());
            return ResponseEntity.ok(Map.of(
                    "conversation", conversation,
                    "currentUser", currentUser,
                    "otherUser", otherUser
            ));
        } catch (RuntimeException e) {
            List<Message> conversation = messageService.getConversation(currentUser.getId(), otherUser.getId());
            return ResponseEntity.badRequest().body(Map.of(
                    "conversation", conversation,
                    "currentUser", currentUser,
                    "otherUser", otherUser,
                    "error", e.getMessage()
            ));
        }
    }

    // Lấy trực tiếp từ session, không cần query lại DB
    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}