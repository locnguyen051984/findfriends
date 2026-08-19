package com.phaithanhcong.user.restcontroller;

import com.phaithanhcong.user.dto.ConversationResponse;
import com.phaithanhcong.user.dto.UserResponse;
import com.phaithanhcong.user.model.Message;
import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.repository.UserRepository;
import com.phaithanhcong.user.service.MessageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        List<Message> conversation = messageService.userGetConversation(currentUser.getId(), otherUser.getId());

        return ResponseEntity.ok(new ConversationResponse(conversation, UserResponse.fromEntity(currentUser), UserResponse.fromEntity(otherUser)));
    }

    @PostMapping("/{otherUserId}")
    public ResponseEntity<?> userSendMessage(@PathVariable Long otherUserId,
                                          @RequestParam String content,
                                          HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        UserResponse currentUserDto = UserResponse.fromEntity(currentUser);
        UserResponse otherUserDto = UserResponse.fromEntity(otherUser);

        try {
            messageService.userSendMessage(currentUser, otherUser, content);
            List<Message> conversation = messageService.userGetConversation(currentUser.getId(), otherUser.getId());
            return ResponseEntity.ok(new ConversationResponse(conversation, currentUserDto, otherUserDto));
        } catch (RuntimeException e) {
            List<Message> conversation = messageService.userGetConversation(currentUser.getId(), otherUser.getId());
            return ResponseEntity.badRequest().body(
                    new ConversationResponse(conversation, currentUserDto, otherUserDto, e.getMessage())
            );
        }
    }

    // Lấy trực tiếp từ session, không cần query lại DB
    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
