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

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

        
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "sentAt")
    private LocalDateTime sentAt;


}
