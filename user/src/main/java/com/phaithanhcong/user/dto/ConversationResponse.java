package com.phaithanhcong.user.dto;

import com.phaithanhcong.user.model.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ConversationResponse {
    private List<Message> conversation;
    private UserResponse currentUser;
    private UserResponse otherUser;
    private String error; // null khi thành công

    public ConversationResponse(List<Message> conversation, UserResponse currentUser, UserResponse otherUser) {
        this(conversation, currentUser, otherUser, null);
    }
}