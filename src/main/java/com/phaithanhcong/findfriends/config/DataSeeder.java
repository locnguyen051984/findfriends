package com.phaithanhcong.findfriends.config;

import com.phaithanhcong.findfriends.model.Role;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.repository.MessageRepository;
import com.phaithanhcong.findfriends.repository.RoleRepository;
import com.phaithanhcong.findfriends.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        messageRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ADMIN");
        if (adminRole == null) {
            adminRole = roleRepository.save(Role.builder().name("ADMIN").build());
        }

        Role userRole = roleRepository.findByName("USER");
        if (userRole == null) {
            userRole = roleRepository.save(Role.builder().name("USER").build());
        }

        userRepository.save(User.builder()
                .userName("admin")
                .password("1")
                .email("admin@gmail.com")
                .premium(false)
                .role(adminRole)
                .build());

        for (int i = 1; i <= 9; i++) {
            userRepository.save(User.builder()
                    .userName("test" + i)
                    .password("1")
                    .email(i + "@gmail.com")
                    .premium(false)
                    .role(userRole)
                    .build());
        }
    }
}