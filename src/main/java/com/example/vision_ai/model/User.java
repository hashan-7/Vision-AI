package com.example.vision_ai.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String googleId;

    private String name;

    @Column(unique = true)
    private String email;

    private String pictureUrl;

    private int dailyCreditsRemaining = 10;

    private LocalDateTime lastImageGeneratedAt;

    private LocalDateTime dailyCreditsRefreshedAt;

    @PrePersist
    protected void onCreate() {
        dailyCreditsRefreshedAt = LocalDateTime.now();
    }
}