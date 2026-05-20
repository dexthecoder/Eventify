package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDto {

    @NotBlank(message = "Yorum metni boş olamaz")
    @Size(max = 500, message = "Yorum en fazla 500 karakter olabilir")
    private String text;
}