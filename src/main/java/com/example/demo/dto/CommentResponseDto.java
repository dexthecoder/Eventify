package com.example.demo.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentResponseDto {
    private Integer id;
    private String text;
    private LocalDateTime creationDate;
    private LocalDateTime updatedDate;
    private UserResponseDto creator; // Yorumu yazan kişinin güvenli bilgileri
}