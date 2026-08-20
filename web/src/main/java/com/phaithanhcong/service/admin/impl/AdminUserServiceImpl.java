package com.phaithanhcong.service.admin.impl;

import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import com.phaithanhcong.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    public List<User> adminGetAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void adminTogglePremium(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPremium(!user.isPremium());
            userRepository.save(user);
        }
    }
}
