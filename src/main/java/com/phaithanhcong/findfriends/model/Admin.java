package com.phaithanhcong.findfriends.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="admins")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="admin_name", unique = true, nullable = false)
    private String adminName;

    @Column(name="password", nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;
}
