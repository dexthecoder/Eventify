package com.example.demo.controller;

import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.ParticipationService;
import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("participation")
@RequiredArgsConstructor
public class ParticipationRestController {

    private final ParticipationService participationService;

    @PostMapping("/{eventId}/join")
    public ResponseEntity joinEvent(@PathVariable Integer eventId) {
        return participationService.joinEvent(eventId);
    }

    @DeleteMapping("/{eventId}/leave")
    public ResponseEntity leaveEvent(@PathVariable Integer eventId) {
        return participationService.leaveEvent(eventId);
    }

    @GetMapping("/{eventId}/users")
    public ResponseEntity getEventParticipants(@PathVariable Integer eventId) {
        return participationService.getEventParticipants(eventId);
    }

    @GetMapping("/my-participations")
    public ResponseEntity getMyParticipatedEvents(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "executionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) ECategory category,
            @RequestParam(required = false) EStatus status) {

        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));
        }

        return participationService.getMyParticipatedEvents(
                sessionUser.getCid(), page, size, sortBy, direction, search, category, status
        );
    }

    @DeleteMapping("/{eventId}/ban/{userId}")
    public ResponseEntity banUser(
            @PathVariable Integer eventId,
            @PathVariable Integer userId,
            HttpServletRequest request) {

        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));
        }

        return participationService.banUserFromEvent(eventId, userId, sessionUser.getCid());
    }
}