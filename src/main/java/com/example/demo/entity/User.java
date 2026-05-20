package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cid;

    @Column(length = 100)
    private String name;

    @Column(length = 100)
    private String surname;

    @Column(unique = true, length = 200)
    private String email;

    @Column(unique = true, length = 15)
    private String phone;

    private boolean enabled;

    @Column(length = 1000)
    private String password;

    // kullanıcının katılımları participation tablosu üzerinden çekilecek
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participation> participations = new HashSet<>();

    // Beğendiği etkinlikler doğrudan manytomany ile çekilecek
    @ManyToMany(mappedBy = "likers")
    private Set<Event> likedEvents = new HashSet<>();
}