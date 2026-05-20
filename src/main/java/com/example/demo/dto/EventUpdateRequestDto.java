package com.example.demo.dto;

import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventUpdateRequestDto {

    @Size(min = 3, max = 150, message = "Etkinlik başlığı 3 ile 150 karakter arasında olmalıdır")
    private String title;

    @Size(max = 2000, message = "Açıklama en fazla 2000 karakter olabilir")
    private String description;

    @Future(message = "Etkinlik tarihi gelecekte bir zaman olmalıdır")
    private LocalDateTime executionDate;

    @Size(max = 250, message = "Konum en fazla 250 karakter olabilir")
    private String location;

    private ECategory category;
    private EStatus status;
}