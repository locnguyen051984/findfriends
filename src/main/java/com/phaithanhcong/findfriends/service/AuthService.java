package com.phaithanhcong.findfriends.service;

import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

    // 1. Phải có từ khóa 'final' để @RequiredArgsConstructor làm việc và inject UserRepository
    private final UserRepository userRepository;

    //Đăng ký
    public boolean register(String userName, String password, String email, boolean premium){
        if(userName == null || userName.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty()) {
            return false;
        }
        if (userRepository.findByUserName(userName).isPresent()) {
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

    //Đăng nhập
    public User login(String userName, String password) {
        if (userName == null || userName.isEmpty() || password == null || password.isEmpty()) {
            return null; // input rỗng -> không cho đăng nhập
        }

        // Sửa: dùng orElse(null) thay vì orElseThrow, vì orElseThrow ném exception
        // sẽ làm crash app (lỗi 500) thay vì trả về thông báo lỗi đẹp cho user
        User user = userRepository.findByUserName(userName).orElse(null);
        if (user == null) {
            return null; // không tìm thấy user -> đăng nhập thất bại (không crash)
        }

        if (user.getPassword().equals(password)) {
            return user;
        }
        return null; // sai password
    }

    // Verify username + email có khớp 1 tài khoản trong DB không
    public boolean verifyAccount(String userName, String email) {
        if (userName == null || userName.isEmpty() || email == null || email.isEmpty()) {
            return false;
        }
        return userRepository.findByUserNameAndEmail(userName, email).isPresent();
    }

    // Đặt lại password mới cho user (chỉ gọi sau khi đã verifyAccount thành công)
    public boolean resetPassword(String userName, String newPassword) {
        if (userName == null || userName.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return false;
        }
        User user = userRepository.findByUserName(userName).orElse(null);
        if (user == null) {
            return false;
        }
        user.setPassword(newPassword);
        userRepository.save(user);
        return true;
    }
}