package com.phaithanhcong.user.service;

import com.phaithanhcong.user.model.CallLog;
import com.phaithanhcong.user.model.Message;
import com.phaithanhcong.user.model.User;
import com.phaithanhcong.user.repository.CallLogRepository;
import com.phaithanhcong.user.repository.MessageRepository;
import com.phaithanhcong.user.repository.UserRepository;
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
