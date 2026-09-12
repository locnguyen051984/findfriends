package com.phaithanhcong.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content")
    private String content;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private MessageType messageType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "sentAt")
    private LocalDateTime sentAt;

    @Column(name = "call_log_id")
    private Long callLogId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
}