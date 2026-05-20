package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Katılım sağlayan kullanıcı
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_cid", referencedColumnName = "cid", nullable = false)
    private User user;

    // Katılım sağlanan etkinlik
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", nullable = false)
    private Event event;

    // Katılımın yapıldığı tarih ve saat
    @Column(nullable = false)
    private LocalDateTime participationDate;

    // Yeni bir katılım oluşturulduğunda tarihi otomatik atamak için
    @PrePersist
    protected void onCreate() {
        this.participationDate = LocalDateTime.now();
    }
}