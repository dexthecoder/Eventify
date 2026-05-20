package com.example.demo.dto;

import com.example.demo.util.ECategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventCreateRequestDto {

    @NotBlank(message = "Etkinlik başlığı boş olamaz")
    @Size(min = 3, max = 150, message = "Etkinlik başlığı 3 ile 150 karakter arasında olmalıdır")
    private String title;

    @NotBlank(message = "Açıklama boş olamaz")
    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    @NotNull(message = "Etkinlik tarihi boş olamaz")
    @Future(message = "Etkinlik tarihi gelecekte bir zaman olmalıdır")
    private LocalDateTime executionDate;

    @NotBlank(message = "Konum bilgisi boş olamaz")
    @Size(max = 250, message = "Konum en fazla 250 karakter olabilir")
    private String location;

    @NotNull(message = "Kategori seçilmelidir")
    private ECategory category;
}