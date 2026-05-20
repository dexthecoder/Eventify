package com.example.demo.controller;

import com.example.demo.dto.UserLoginRequestDto;
import com.example.demo.dto.UserRegisterRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserRestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // ============ REGISTER ============

    @Test
    public void testRegister_ShouldReturnOk() throws Exception {
        UserRegisterRequestDto dto = new UserRegisterRequestDto();
        dto.setName("Polat");
        dto.setSurname("Test");
        dto.setEmail("polat@test.com");
        dto.setPhone("05551234567");
        dto.setPassword("Test123!");

        UserResponseDto response = new UserResponseDto();
        response.setCid(1);
        response.setEmail("polat@test.com");

        when(userService.register(any(UserRegisterRequestDto.class)))
                .thenReturn(ResponseEntity.ok(response));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("polat@test.com"));
    }

    @Test
    public void testRegister_WhenInvalidEmail_ShouldReturnBadRequest() throws Exception {
        UserRegisterRequestDto dto = new UserRegisterRequestDto();
        dto.setName("Polat");
        dto.setSurname("Test");
        dto.setEmail("gecersiz-email"); // invalid
        dto.setPhone("05551234567");
        dto.setPassword("Test123!");

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_WhenWeakPassword_ShouldReturnBadRequest() throws Exception {
        UserRegisterRequestDto dto = new UserRegisterRequestDto();
        dto.setName("Polat");
        dto.setSurname("Test");
        dto.setEmail("polat@test.com");
        dto.setPhone("05551234567");
        dto.setPassword("zayif"); // büyük harf, rakam, özel karakter yok

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegister_WhenEmailAlreadyExists_ShouldReturnBadRequest() throws Exception {
        UserRegisterRequestDto dto = new UserRegisterRequestDto();
        dto.setName("Polat");
        dto.setSurname("Test");
        dto.setEmail("polat@test.com");
        dto.setPhone("05551234567");
        dto.setPassword("Test123!");

        when(userService.register(any(UserRegisterRequestDto.class)))
                .thenReturn(ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Bu email veya telefon zaten kullanımda.")));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ============ LOGIN ============

    @Test
    public void testLogin_ShouldReturnOk() throws Exception {
        UserLoginRequestDto dto = new UserLoginRequestDto();
        dto.setUsername("polat@test.com");
        dto.setPassword("Test123!");

        UserResponseDto response = new UserResponseDto();
        response.setCid(1);

        when(userService.login(any(UserLoginRequestDto.class)))
                .thenReturn(ResponseEntity.ok(response));

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cid").value(1));
    }

    @Test
    public void testLogin_WhenWrongPassword_ShouldReturnBadRequest() throws Exception {
        UserLoginRequestDto dto = new UserLoginRequestDto();
        dto.setUsername("polat@test.com");
        dto.setPassword("YanlisSifre1!");

        when(userService.login(any(UserLoginRequestDto.class)))
                .thenReturn(ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Kullanıcı adı veya şifre hatalı.")));

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLogin_WhenBlankUsername_ShouldReturnBadRequest() throws Exception {
        UserLoginRequestDto dto = new UserLoginRequestDto();
        dto.setUsername("");
        dto.setPassword("Test123!");

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ============ LOGOUT ============

    @Test
    public void testLogout_ShouldReturnOk() throws Exception {
        when(userService.logout())
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Oturum kapatıldı.")));

        mockMvc.perform(get("/user/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}