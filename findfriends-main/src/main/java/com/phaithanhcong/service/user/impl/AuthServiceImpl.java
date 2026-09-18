package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.AuthService;

import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RequiredArgsConstructor
@Service


public class AuthServiceImpl implements AuthService {

    // 1. Phải có từ khóa 'final' để @RequiredArgsConstructor làm việc và inject UserRepository
    private final UserRepository userRepository;

    // BCrypt dùng để mã hóa và kiểm tra mật khẩu
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    //Đăng ký
    public boolean userRegister(String userName, String password, String email, boolean premium){
        if(userName == null || userName.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty()) {
            return false;
        }
        if (userRepository.findByUserName(userName).isPresent()) {
            return false;
        }
        if (!userRepository.findAllByEmail(email).isEmpty()) {
            return false;
        }
        // Mã hóa mật khẩu trước khi lưu vào database
        String encodedPassword = passwordEncoder.encode(password);

        userRepository.save(
                User.builder()
                        .userName(userName)
                        .password(encodedPassword)
                        .email(email)
                        .premium(premium)
                        .status("ACTIVE")
                        .role("USER")
                        .build()
        );
        return true;
    }

    //Đăng nhập
    public User userLogin(String userName, String password) {
        if (userName == null || userName.isEmpty() || password == null || password.isEmpty()) {
            return null; // input rỗng -> không cho đăng nhập
        }

        // Sửa: dùng orElse(null) thay vì orElseThrow, vì orElseThrow ném exception
        // sẽ làm crash app (lỗi 500) thay vì trả về thông báo lỗi đẹp cho user
        User user = userRepository.findByUserName(userName).orElse(null);
        if (user == null) {
            return null; // không tìm thấy user -> đăng nhập thất bại (không crash)
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return null;
        }

        // Kiểm tra mật khẩu người dùng nhập với mật khẩu BCrypt trong database
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null; // sai password
    }

    // Verify username + email có khớp 1 tài khoản trong DB không
    public boolean userVerifyAccount(String userName, String email) {
        if (userName == null || userName.isEmpty() || email == null || email.isEmpty()) {
            return false;
        }
        return userRepository.findByUserNameAndEmail(userName, email).isPresent();
    }

    // Đặt lại password mới cho user (chỉ gọi sau khi đã verifyAccount thành công)
    public boolean userResetPassword(String userName, String newPassword) {
        if (userName == null || userName.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return false;
        }
        User user = userRepository.findByUserName(userName).orElse(null);
        if (user == null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
    // =====================================================
    // TẠO TOKEN ĐỂ RESET MẬT KHẨU
    // =====================================================

    private final java.util.Map<String, String> resetTokens =
            new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.Map<String, Long> resetTokenExpiry =
            new java.util.concurrent.ConcurrentHashMap<>();

    public String createResetToken(String userName, String email) {

        if (userName == null || userName.isEmpty()
                || email == null || email.isEmpty()) {
            return null;
        }

        User user = userRepository
                .findByUserNameAndEmail(userName, email)
                .orElse(null);

        if (user == null) {
            return null;
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return null;
        }

        // Tạo token ngẫu nhiên
        String token = java.util.UUID.randomUUID().toString();

        // Token tương ứng với username
        resetTokens.put(token, userName);

        // Token có hiệu lực 15 phút
        long expiryTime =
                System.currentTimeMillis()
                        + (15 * 60 * 1000);

        resetTokenExpiry.put(token, expiryTime);

        return token;
    }

    // =====================================================
    // KIỂM TRA TOKEN
    // =====================================================

    public boolean isValidResetToken(String token) {

        if (token == null || token.isEmpty()) {
            return false;
        }

        String userName = resetTokens.get(token);

        Long expiryTime = resetTokenExpiry.get(token);

        if (userName == null || expiryTime == null) {
            return false;
        }

        // Token hết hạn
        if (System.currentTimeMillis() > expiryTime) {

            resetTokens.remove(token);
            resetTokenExpiry.remove(token);

            return false;
        }

        return true;
    }

    // =====================================================
    // RESET PASSWORD BẰNG TOKEN
    // =====================================================

    public boolean resetPasswordByToken(
            String token,
            String newPassword) {

        if (!isValidResetToken(token)) {
            return false;
        }

        if (newPassword == null
                || newPassword.length() < 8) {
            return false;
        }

        String userName = resetTokens.get(token);

        User user = userRepository
                .findByUserName(userName)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return false;
        }

        // Không cho dùng lại mật khẩu cũ
        if (passwordEncoder.matches(
                newPassword,
                user.getPassword())) {

            return false;
        }

        // Mã hóa password mới
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        // Token chỉ được sử dụng một lần
        resetTokens.remove(token);
        resetTokenExpiry.remove(token);

        return true;
    }

    // =====================================================
    // ĐỔI PASSWORD KHI ĐÃ ĐĂNG NHẬP
    // =====================================================

    public boolean changePassword(
            String userName,
            String currentPassword,
            String newPassword) {

        if (userName == null || userName.isEmpty()
                || currentPassword == null
                || currentPassword.isEmpty()
                || newPassword == null
                || newPassword.length() < 8) {

            return false;
        }

        User user = userRepository
                .findByUserName(userName)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return false;
        }

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(
                currentPassword,
                user.getPassword())) {

            return false;
        }

        // Không cho đổi thành mật khẩu cũ
        if (passwordEncoder.matches(
                newPassword,
                user.getPassword())) {

            return false;
        }

        // Mã hóa mật khẩu mới
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        return true;
    }
}