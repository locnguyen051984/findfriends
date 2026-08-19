package com.phaithanhcong.admin.service.impl;

import com.phaithanhcong.admin.model.User;
import com.phaithanhcong.admin.repository.UserRepository;
import com.phaithanhcong.admin.service.AdminUserService;
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
