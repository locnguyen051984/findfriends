package com.phaithanhcong.service.user;

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

public interface LocationService {
    double userHaversine(double lat1, double lon1, double lat2, double lon2);

    Optional<String> userRecordLoginAndCheckAnomaly(User user, double lat, double lon);

    List<Map<String, Object>> userGetNearbyList(User currentUser, List<User> otherUsers);
}
