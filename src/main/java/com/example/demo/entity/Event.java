package com.example.demo.entity;

import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus; // EStatus import edildi
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @org.hibernate.annotations.CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime creationDate; // Otomatik atanacak oluşturulma tarihi

    private LocalDateTime executionDate; // Kullanıcının gireceği etkinlik tarihi

    @Column(length = 250)
    private String location;

    @Enumerated(EnumType.STRING)
    private ECategory category;

    // Durum bilgisi
    @Enumerated(EnumType.STRING)
    private EStatus status;

    @Column(length = 500)
    private String imagePath;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participation> participations = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "event_likes",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> likers = new HashSet<>();
}