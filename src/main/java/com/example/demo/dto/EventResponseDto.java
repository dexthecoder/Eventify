package com.example.demo.dto;

import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponseDto {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime creationDate;
    private LocalDateTime executionDate;
    private String location;
    private ECategory category;
    private EStatus status;
    private String imagePath;

    private UserResponseDto creator;

    private long likeCount;
    private long participantCount;
}