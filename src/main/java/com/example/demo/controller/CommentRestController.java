package com.example.demo.controller;

import com.example.demo.dto.CommentRequestDto;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("comment")
@RequiredArgsConstructor
public class CommentRestController {

    private final CommentService commentService;

    @PostMapping("/{eventId}")
    public ResponseEntity addComment(
            @PathVariable Integer eventId,
            @Valid @RequestBody CommentRequestDto dto) {
        return commentService.addComment(eventId, dto);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity getEventComments(@PathVariable Integer eventId) {
        return commentService.getCommentsOfEvent(eventId);
    }

    @PutMapping("/{id}")
    public ResponseEntity updateComment(
            @PathVariable Integer id,
            @Valid @RequestBody CommentRequestDto dto) {
        return commentService.updateComment(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deleteComment(@PathVariable Integer id) {
        return commentService.deleteComment(id);
    }
}