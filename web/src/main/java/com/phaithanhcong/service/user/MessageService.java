package com.phaithanhcong.service.user;

import com.phaithanhcong.model.CallLog;
import com.phaithanhcong.model.Message;
import com.phaithanhcong.model.User;
import com.phaithanhcong.repository.CallLogRepository;
import com.phaithanhcong.repository.MessageRepository;
import com.phaithanhcong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MessageService {
    Message userSendMessage(User sender, User receiver, String content);
    List<Message> userGetConversation(Long senderId, Long receiverId);
    List<Map<String, Object>> userBuildTimeline(User currentUser, User otherUser);
}
