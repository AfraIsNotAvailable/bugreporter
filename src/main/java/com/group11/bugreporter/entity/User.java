package com.group11.bugreporter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.group11.bugreporter.entity.enums.Role;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //aici se poate scoate nullable pentru ca ID ii deja not null
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    //aici vine de fapt hash-ul parolei
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean banned;

    //asta poate imi trebuie la banning
    @Column(nullable = false)
    private LocalDateTime createdAt;

    //metoda ca sa pun timpu la createdAt
    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
    }



}
