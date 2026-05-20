package com.example.demo.controller;

import com.example.demo.dto.EventCreateRequestDto;
import com.example.demo.dto.EventUpdateRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.CommentService;
import com.example.demo.service.EventService;
import com.example.demo.util.ECategory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("event")
public class EventRestController {

    private final EventService eventService;
    private final CommentService commentService;

    @PostMapping("create")
    public ResponseEntity createEvent(
            @Valid @ModelAttribute EventCreateRequestDto eventDto,
            @RequestParam(value = "image", required = false) MultipartFile file) {

        return eventService.createEvent(eventDto, file);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity deleteEvent(@PathVariable Integer id) {
        return eventService.deleteEvent(id);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity updateEvent(
            @PathVariable Integer id,
            @Valid @ModelAttribute EventUpdateRequestDto updateDto,
            @RequestParam(value = "image", required = false) MultipartFile file) {

        return eventService.updateEvent(id, updateDto, file);
    }

    @GetMapping("/list")
    public ResponseEntity getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "executionDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) ECategory category) {

        return eventService.getAllEvents(page, size, sortBy, direction, search, category);
    }

    @GetMapping("/my-events")
    public ResponseEntity getMyEvents(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) ECategory category) {

        // Kullanıcı giriş yapmış mı?
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));
        }

        // Kullanıcının ID'si servise gönderiliyor
        return eventService.getUsersEvents(
                sessionUser.getCid(), page, size, sortBy, direction, search, category
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity getEventById(@PathVariable Integer id) {
        return eventService.getEventById(id);
    }
}