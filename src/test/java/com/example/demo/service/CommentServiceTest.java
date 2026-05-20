package com.example.demo.service;

import com.example.demo.dto.CommentRequestDto;
import com.example.demo.dto.CommentResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Event;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ModelMapper model;
    @Mock private HttpServletRequest request;
    @Mock private HttpSession session;

    @InjectMocks
    private CommentService commentService;

    private UserResponseDto sessionUser;
    private User user;
    private User otherUser;
    private CommentRequestDto dto;

    @BeforeEach
    void setUp() {
        sessionUser = new UserResponseDto();
        sessionUser.setCid(1);

        user = new User();
        user.setCid(1);

        otherUser = new User();
        otherUser.setCid(999);

        dto = new CommentRequestDto();
        dto.setText("Harika yorum");
    }

    // ============ ADD ============

    @Test
    void addComment_WhenNoSession_ShouldReturnUnauthorized() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity response = commentService.addComment(1, dto);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void addComment_WhenEventNotFound_ShouldReturnNotFound() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity response = commentService.addComment(999, dto);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void addComment_WhenValid_ShouldSaveAndReturnDto() {
        Event event = new Event();
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Comment saved = new Comment();
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);
        when(model.map(saved, CommentResponseDto.class)).thenReturn(new CommentResponseDto());

        ResponseEntity response = commentService.addComment(1, dto);

        assertEquals(200, response.getStatusCode().value());
        verify(commentRepository).save(any(Comment.class));
    }

    // ============ UPDATE ============

    @Test
    void updateComment_WhenCommentNotFound_ShouldReturnNotFound() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(commentRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity response = commentService.updateComment(999, dto);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateComment_WhenNotOwner_ShouldReturnForbidden() {
        Comment comment = new Comment();
        comment.setCreator(otherUser);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(commentRepository.findById(1)).thenReturn(Optional.of(comment));

        ResponseEntity response = commentService.updateComment(1, dto);

        assertEquals(403, response.getStatusCode().value());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_WhenOwner_ShouldUpdateAndReturnDto() {
        Comment comment = new Comment();
        comment.setCreator(user);
        comment.setText("Eski yorum");

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(commentRepository.findById(1)).thenReturn(Optional.of(comment));
        when(model.map(comment, CommentResponseDto.class)).thenReturn(new CommentResponseDto());

        ResponseEntity response = commentService.updateComment(1, dto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Harika yorum", comment.getText());
        verify(commentRepository).save(comment);
    }

    // ============ DELETE ============

    @Test
    void deleteComment_WhenNotCommentOwnerAndNotEventOwner_ShouldReturnForbidden() {
        Event event = new Event();
        event.setCreator(otherUser); // event başkasının

        Comment comment = new Comment();
        comment.setCreator(otherUser); // yorum başkasının
        comment.setEvent(event);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(commentRepository.findById(1)).thenReturn(Optional.of(comment));

        ResponseEntity response = commentService.deleteComment(1);

        assertEquals(403, response.getStatusCode().value());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_WhenCommentOwner_ShouldDelete() {
        Event event = new Event();
        event.setCreator(otherUser);

        Comment comment = new Comment();
        comment.setCreator(user); // yorum kendinin
        comment.setEvent(event);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(commentRepository.findById(1)).thenReturn(Optional.of(comment));

        ResponseEntity response = commentService.deleteComment(1);

        assertEquals(200, response.getStatusCode().value());
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_WhenEventOwner_ShouldDelete() {
        Event event = new Event();
        event.setCreator(user); // event kendinin

        Comment comment = new Comment();
        comment.setCreator(otherUser); // yorum başkasının
        comment.setEvent(event);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(commentRepository.findById(1)).thenReturn(Optional.of(comment));

        ResponseEntity response = commentService.deleteComment(1);

        assertEquals(200, response.getStatusCode().value());
        verify(commentRepository).delete(comment);
    }

    // ============ GET ============

    @Test
    void getCommentsOfEvent_ShouldReturnList() {
        when(commentRepository.findByEvent_IdOrderByCreationDateDesc(1))
                .thenReturn(Collections.emptyList());

        ResponseEntity response = commentService.getCommentsOfEvent(1);

        assertEquals(200, response.getStatusCode().value());
    }
}
