package com.phaithanhcong.user.controller;

import com.phaithanhcong.user.dto.HomeResponse;
import com.phaithanhcong.user.dto.UserResponse;
import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class HomeControllerREST {

    private final UserRepository userRepository;

    @GetMapping("/home")
    public ResponseEntity<?> home(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<UserResponse> otherUsers = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .map(UserResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(new HomeResponse(user.getUserName(), otherUsers));
    }
}