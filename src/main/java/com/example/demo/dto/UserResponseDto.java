package com.example.demo.dto;

import lombok.Data;

@Data
public class UserResponseDto {
    private Integer cid;
    private String name;
    private String surname;
    private String email;
    private String phone;
}