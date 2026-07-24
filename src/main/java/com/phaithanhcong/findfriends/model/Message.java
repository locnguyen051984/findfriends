package com.phaithanhcong.findfriends.model;


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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender-id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver-id")
    private User receiver;

    @Column(name = "sentAt")
    private LocalDateTime sentAt;


}
