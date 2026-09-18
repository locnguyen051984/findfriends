package com.phaithanhcong.service.user;

import com.phaithanhcong.model.User;

public interface AuthService {

    boolean userRegister(
            String userName,
            String password,
            String email,
            boolean premium
    );

    User userLogin(
            String userName,
            String password
    );

    boolean userVerifyAccount(
            String userName,
            String email
    );

    boolean userResetPassword(
            String userName,
            String newPassword
    );

    String createResetToken(
            String userName,
            String email
    );

    boolean isValidResetToken(
            String token
    );

    boolean resetPasswordByToken(
            String token,
            String newPassword
    );

    boolean changePassword(
            String userName,
            String currentPassword,
            String newPassword
    );
}