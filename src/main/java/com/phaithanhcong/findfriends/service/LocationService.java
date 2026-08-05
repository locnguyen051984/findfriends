package com.phaithanhcong.findfriends.service;

import com.phaithanhcong.findfriends.model.LoginLocation;
import com.phaithanhcong.findfriends.model.User;
import com.phaithanhcong.findfriends.model.UserLocation;
import com.phaithanhcong.findfriends.repository.LoginLocationRepository;
import com.phaithanhcong.findfriends.repository.UserLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class LocationService {

    private final LoginLocationRepository loginLocationRepository;
    private final UserLocationRepository userLocationRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int HISTORY_SIZE = 10;
    private static final double ANOMALY_THRESHOLD_KM = 100.0;

    public double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public Optional<String> recordLoginAndCheckAnomaly(User user, double lat, double lon) {

        List<LoginLocation> recentLogins = loginLocationRepository
                .findByUserIdOrderByLoginAtDesc(user.getId(), PageRequest.of(0, HISTORY_SIZE));

        Optional<String> warning = Optional.empty();

        if (recentLogins.size() >= HISTORY_SIZE) {
            double avgLat = recentLogins.stream().mapToDouble(LoginLocation::getLatitude).average().orElse(lat);
            double avgLon = recentLogins.stream().mapToDouble(LoginLocation::getLongitude).average().orElse(lon);
            double distance = haversine(avgLat, avgLon, lat, lon);

            if (distance > ANOMALY_THRESHOLD_KM) {
                warning = Optional.of(String.format(
                        "Cảnh báo bảo mật: Tài khoản vừa đăng nhập từ vị trí cách xa khoảng %.0f km so với vị trí thường dùng. Nếu không phải bạn, hãy đổi mật khẩu ngay!",
                        distance
                ));
            }
        }

        loginLocationRepository.save(
                LoginLocation.builder()
                        .userId(user.getId())
                        .latitude(lat)
                        .longitude(lon)
                        .loginAt(LocalDateTime.now())
                        .build()
        );

        UserLocation location = userLocationRepository.findByUserId(user.getId())
                .orElse(UserLocation.builder().userId(user.getId()).build());
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setUpdatedAt(LocalDateTime.now());
        userLocationRepository.save(location);

        return warning;
    }

    public List<Map<String, Object>> getNearbyList(User currentUser, List<User> otherUsers) {
        List<Map<String, Object>> result = new ArrayList<>();
        Optional<UserLocation> currentLoc = userLocationRepository.findByUserId(currentUser.getId());

        for (User other : otherUsers) {
            Optional<UserLocation> otherLoc = userLocationRepository.findByUserId(other.getId());

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", other.getId());
            entry.put("username", other.getUserName());

            if (currentLoc.isPresent() && otherLoc.isPresent()) {
                double km = haversine(
                        currentLoc.get().getLatitude(), currentLoc.get().getLongitude(),
                        otherLoc.get().getLatitude(), otherLoc.get().getLongitude());
                entry.put("distance", String.format("%.1f km", km));
            } else {
                entry.put("distance", "Chưa có vị trí");
            }

            result.add(entry);
        }
        return result;
    }
}