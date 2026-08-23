package com.phaithanhcong.service.user;

import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

public interface UserService {
    List<User> userGetAllUsers();
    List<User> userGetAllUsersByEmail(String email);
}
