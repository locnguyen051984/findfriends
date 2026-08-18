package com.phaithanhcong.user.service;

import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    // HIển thị tất cả
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Lọc hiển thị tất cả user cùng 1 email
    public List<User> getAllUsersByEmail(String email){
        return userRepository.findAllByEmail(email);
    }


}
