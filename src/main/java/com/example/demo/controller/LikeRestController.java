package com.example.demo.controller;

import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.LikeService;
import com.example.demo.util.ECategory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("like")
@RequiredArgsConstructor
public class LikeRestController {

    private final LikeService likeService;

    @PostMapping("/{eventId}")
    public ResponseEntity likeEvent(@PathVariable Integer eventId) {
        return likeService.likeEvent(eventId);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity unlikeEvent(@PathVariable Integer eventId) {
        return likeService.unlikeEvent(eventId);
    }

    @GetMapping("/count/{eventId}")
    public ResponseEntity getEventLikeCount(@PathVariable Integer eventId) {
        return likeService.getEventLikeCount(eventId);
    }

    @GetMapping("/my-likes")
    public ResponseEntity getMyLikedEvents(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "executionDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) ECategory category) {

        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));
        }

        return likeService.getMyLikedEvents(
                sessionUser.getCid(), page, size, sortBy, direction, search, category
        );
    }
}