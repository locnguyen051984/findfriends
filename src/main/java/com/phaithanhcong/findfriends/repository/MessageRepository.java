package com.phaithanhcong.findfriends.repository;


import com.phaithanhcong.findfriends.model.Message;
import com.phaithanhcong.findfriends.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByContent(User sender, User receiver, LocalDateTime timeStamp);


    List<Message> findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderBySentAtAsc(Long userId1, Long userId2);
}
