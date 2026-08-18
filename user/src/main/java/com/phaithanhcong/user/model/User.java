package com.phaithanhcong.user.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user-name", unique = true)
    private String userName;

    @Column(name="password")
    private String password;

    @Column(unique = true)
    private String email;

    @Column
    private boolean premium;

}
