package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.UserService;

import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service


public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    // HIển thị tất cả
    public List<User> userGetAllUsers() {
        return userRepository.findAll();
    }

    // Lọc hiển thị tất cả user cùng 1 email
    public List<User> userGetAllUsersByEmail(String email){
        return userRepository.findAllByEmail(email);
    }


}
