// package com.phaithanhcong.findfriends.config;

// import com.phaithanhcong.findfriends.model.Admin;
// import com.phaithanhcong.findfriends.model.User;
// import com.phaithanhcong.findfriends.repository.AdminRepository;
// import com.phaithanhcong.findfriends.repository.LoginLocationRepository;
// import com.phaithanhcong.findfriends.repository.MessageRepository;
// import com.phaithanhcong.findfriends.repository.UserRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;

// @Component
// @RequiredArgsConstructor
// public class DataSeeder implements CommandLineRunner {

// private final UserRepository userRepository;
// private final AdminRepository adminRepository;
// private final MessageRepository messageRepository;
// private final LoginLocationRepository locationRepository;

// @Override
// public void run(String... args) {
// messageRepository.deleteAll();
// userRepository.deleteAll();
// adminRepository.deleteAll();
// locationRepository.deleteAll();

// adminRepository.save(Admin.builder()
// .adminName("admin")
// .password("1")
// .email("admin@gmail.com")
// .build());

// for (int i = 1; i <= 9; i++) {
// userRepository.save(User.builder()
// .userName("test" + i)
// .password("1")
// .email("test" + i + "@gmail.com")
// .premium(false)
// .build());
// }

// // Viết thêm dữ liệu có sẵn message
// }
// }