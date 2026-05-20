package com.example.demo.service;

import com.example.demo.dto.UserLoginRequestDto;
import com.example.demo.dto.UserRegisterRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ModelMapper model;
    @Mock private HttpServletRequest request;
    @Mock private HttpSession session;

    @InjectMocks
    private UserService userService;

    private UserRegisterRequestDto registerDto;
    private UserLoginRequestDto loginDto;

    @BeforeEach
    void setUp() {
        registerDto = new UserRegisterRequestDto();
        registerDto.setName("Polat");
        registerDto.setSurname("Test");
        registerDto.setEmail("polat@test.com");
        registerDto.setPhone("05551234567");
        registerDto.setPassword("Test123!");

        loginDto = new UserLoginRequestDto();
        loginDto.setUsername("polat@test.com");
        loginDto.setPassword("Test123!");
    }

    // ============ REGISTER ============

    @Test
    void register_WhenEmailOrPhoneAlreadyExists_ShouldReturnBadRequest() {
        when(userRepository.findByEmailEqualsOrPhoneEqualsAllIgnoreCase(
                anyString(), anyString()))
                .thenReturn(List.of(new User()));

        ResponseEntity response = userService.register(registerDto);

        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_WhenValidData_ShouldHashPasswordAndSave() {
        when(userRepository.findByEmailEqualsOrPhoneEqualsAllIgnoreCase(
                anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        User mappedUser = new User();
        mappedUser.setPassword("Test123!"); // plain
        when(model.map(registerDto, User.class)).thenReturn(mappedUser);

        User savedUser = new User();
        savedUser.setCid(1);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setCid(1);
        when(model.map(savedUser, UserResponseDto.class)).thenReturn(responseDto);

        ResponseEntity response = userService.register(registerDto);

        assertEquals(200, response.getStatusCode().value());
        // Şifre hash'lendi mi? Plain olmamalı
        assertNotEquals("Test123!", mappedUser.getPassword());
        assertTrue(BCrypt.checkpw("Test123!", mappedUser.getPassword()));
        assertTrue(mappedUser.isEnabled());
        verify(userRepository).save(mappedUser);
    }

    // ============ LOGIN ============

    @Test
    void login_WhenUserNotFound_ShouldReturnBadRequest() {
        when(userRepository.findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(
                anyString(), anyString()))
                .thenReturn(Optional.empty());

        ResponseEntity response = userService.login(loginDto);

        assertEquals(400, response.getStatusCode().value());
        verify(request, never()).getSession();
    }

    @Test
    void login_WhenPasswordWrong_ShouldReturnBadRequest() {
        User user = new User();
        user.setPassword(BCrypt.hashpw("DogruSifre1!", BCrypt.gensalt()));

        when(userRepository.findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(
                anyString(), anyString()))
                .thenReturn(Optional.of(user));

        ResponseEntity response = userService.login(loginDto);

        assertEquals(400, response.getStatusCode().value());
        verify(request, never()).getSession();
    }

    @Test
    void login_WhenCredentialsCorrect_ShouldSetSessionAndReturnUser() {
        User user = new User();
        user.setCid(1);
        user.setPassword(BCrypt.hashpw("Test123!", BCrypt.gensalt()));

        when(userRepository.findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(
                anyString(), anyString()))
                .thenReturn(Optional.of(user));

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setCid(1);
        when(model.map(user, UserResponseDto.class)).thenReturn(responseDto);

        // Session fixation: getSession(false) → null döner (eski session yok)
        when(request.getSession(false)).thenReturn(null);
        // getSession(true) → yeni session döner
        when(request.getSession(true)).thenReturn(session);

        ResponseEntity response = userService.login(loginDto);

        assertEquals(200, response.getStatusCode().value());
        verify(session).setAttribute("user", responseDto);
    }

    // ============ LOGOUT ============

    @Test
    void logout_WhenSessionExists_ShouldInvalidateSession() {
        when(request.getSession(false)).thenReturn(session);

        ResponseEntity response = userService.logout();

        assertEquals(200, response.getStatusCode().value());
        verify(session).invalidate();
    }

    @Test
    void logout_WhenNoSession_ShouldStillReturnOk() {
        when(request.getSession(false)).thenReturn(null);

        ResponseEntity response = userService.logout();

        assertEquals(200, response.getStatusCode().value());
    }
}