package com.phaithanhcong.findfriends.service;

import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {
    private UserRepository userRepository;

    //Đăng ký
    public boolean register(String userName, String password, String email, boolean premium) {
        if(userName == null || userName.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty()) {
            return false;
        }
        if (userRepository.findByUserName(userName) .isPresent()) {
            return false;
        }
        userRepository.save(
                User.builder()
                        .userName(userName)
                        .password(password)
                        .email(email)
                        .premium(premium)
                        .build()
        );
        return true;
    }

    //Đăng nhâpj
    public User login(String userName, String password){
        if(userName == null || userName.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }
        User user = userRepository.findByUserName(userName).orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}