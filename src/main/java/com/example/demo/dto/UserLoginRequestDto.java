package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserLoginRequestDto {

    @NotBlank(message = "Kullanıcı adı (Email/Telefon) boş olamaz")
    @Size(min = 5, max = 100, message = "Kullanıcı adı 5 ile 100 karakter arasında olmalıdır")
    private String username;

    @NotBlank(message = "Şifre boş olamaz")
    @Size(min = 6, max = 30, message = "Şifre 6 ile 30 karakter arasında olmalıdır")
    private String password;
}