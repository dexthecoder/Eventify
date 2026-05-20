package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    // Register için
    List<User> findByEmailEqualsOrPhoneEqualsAllIgnoreCase(String email, String phone);

    // Login için
    Optional<User> findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(String email, String phone);
}