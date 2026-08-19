package com.phaithanhcong.user.service;

import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

public interface UserService {
    List<User> userGetAllUsers();
    List<User> userGetAllUsersByEmail(String email);
}
