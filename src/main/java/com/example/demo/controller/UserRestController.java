package com.example.demo.controller;

import com.example.demo.dto.UserLoginRequestDto;
import com.example.demo.dto.UserRegisterRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("user")
public class UserRestController {

    private final UserService userService;

    @PostMapping("register")
    public ResponseEntity register(@Valid @RequestBody UserRegisterRequestDto userRegisterRequestDto){
        return userService.register(userRegisterRequestDto);
    }

    @PostMapping("login")
    public ResponseEntity login(@Valid @RequestBody UserLoginRequestDto userLoginRequestDto){
        return userService.login(userLoginRequestDto);
    }

    @GetMapping("logout")
    public ResponseEntity logout() {
        return userService.logout();
    }

    @GetMapping("me")
    public ResponseEntity getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));
        }

        UserResponseDto sessionUser = (UserResponseDto) session.getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));
        }

        return ResponseEntity.ok().body(sessionUser);
    }
}