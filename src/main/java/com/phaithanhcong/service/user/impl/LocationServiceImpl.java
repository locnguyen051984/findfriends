package com.phaithanhcong.service.user.impl;

import com.phaithanhcong.service.user.LocationService;
import com.phaithanhcong.model.LoginLocation;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.LoginLocationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service


public class LocationServiceImpl implements LocationService {

    private final LoginLocationRepository loginLocationRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int HISTORY_SIZE = 10;
    private static final double ANOMALY_THRESHOLD_KM = 100.0;
    private static final double NEARBY_RADIUS_KM = 20.0;

    public double userHaversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public Optional<String> userRecordLoginAndCheckAnomaly(User user, double lat, double lon) {

        List<LoginLocation> allLogins = loginLocationRepository.findByUserOrderByLoginAtDesc(user);

        Optional<String> warning = Optional.empty();

        if (allLogins.size() >= HISTORY_SIZE) {
            List<LoginLocation> recentLogins = allLogins.subList(0, HISTORY_SIZE);
            double avgLat = recentLogins.stream().mapToDouble(LoginLocation::getLatitude).average().orElse(lat);
            double avgLon = recentLogins.stream().mapToDouble(LoginLocation::getLongitude).average().orElse(lon);
            double distance = userHaversine(avgLat, avgLon, lat, lon);

            if (distance > ANOMALY_THRESHOLD_KM) {
                warning = Optional.of(String.format(
                        "Cảnh báo bảo mật: Tài khoản vừa đăng nhập từ vị trí cách xa khoảng %.0f km so với vị trí thường dùng. Nếu không phải bạn, hãy đổi mật khẩu ngay!",
                        distance));
            }
        }

        loginLocationRepository.save(
                LoginLocation.builder()
                        .user(user)
                        .latitude(lat)
                        .longitude(lon)
                        .loginAt(LocalDateTime.now())
                        .build());

        List<LoginLocation> updatedLogins = loginLocationRepository.findByUserOrderByLoginAtDesc(user);
        if (updatedLogins.size() > HISTORY_SIZE) {
            loginLocationRepository.deleteAll(updatedLogins.subList(HISTORY_SIZE, updatedLogins.size()));
        }

        return warning;
    }

    public List<Map<String, Object>> userGetNearbyList(User currentUser, List<User> otherUsers) {
        List<Map<String, Object>> result = new ArrayList<>();
        Optional<LoginLocation> currentLoc = loginLocationRepository.findFirstByUserOrderByLoginAtDesc(currentUser);

        for (User other : otherUsers) {
            Optional<LoginLocation> otherLoc = loginLocationRepository.findFirstByUserOrderByLoginAtDesc(other);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", other.getId());
            entry.put("username", other.getUserName());

            if (currentLoc.isPresent() && otherLoc.isPresent()) {
                double km = userHaversine(
                        currentLoc.get().getLatitude(), currentLoc.get().getLongitude(),
                        otherLoc.get().getLatitude(), otherLoc.get().getLongitude());

                if (km <= NEARBY_RADIUS_KM) {
                    entry.put("distance", String.format("%.1f km", km));
                } else {
                    entry.put("distance", "OUT_OF_RANGE");
                }
            } else {
                entry.put("distance", "NO_LOCATION");
            }

            result.add(entry);
        }
        return result;
    }
}