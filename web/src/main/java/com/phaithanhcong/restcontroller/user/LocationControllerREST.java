package com.phaithanhcong.restcontroller.user;

import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.UserRepository;
import com.phaithanhcong.service.user.LocationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationControllerREST {

    private final LocationService locationService;
    private final UserRepository userRepository;

    @PostMapping("/record")
    public ResponseEntity<?> record(@RequestParam double latitude,
                                     @RequestParam double longitude,
                                     HttpSession session) {
        User currentUser = getCurrentUser(session);

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, String> response = new HashMap<>();
        locationService.userRecordLoginAndCheckAnomaly(currentUser, latitude, longitude)
                .ifPresent(warning -> response.put("warning", warning));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/distances")
    public ResponseEntity<?> distances(HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<User> otherUsers = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .toList();

        return ResponseEntity.ok(locationService.userGetNearbyList(currentUser, otherUsers));
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
