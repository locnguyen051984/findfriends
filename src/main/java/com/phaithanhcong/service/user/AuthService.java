package com.phaithanhcong.service.user;

import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

public interface AuthService {
    boolean userRegister(String userName, String password, String email, boolean premium);
    User userLogin(String userName, String password);
    boolean userVerifyAccount(String userName, String email);
    boolean userResetPassword(String userName, String newPassword);
}
