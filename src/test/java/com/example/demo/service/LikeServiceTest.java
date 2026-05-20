package com.example.demo.service;

import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.Event;
import com.example.demo.entity.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ModelMapper model;
    @Mock private EventService eventService; // EKLE
    @Mock private HttpServletRequest request;
    @Mock private HttpSession session;

    @InjectMocks
    private LikeService likeService;

    private UserResponseDto sessionUser;
    private User user;

    @BeforeEach
    void setUp() {
        sessionUser = new UserResponseDto();
        sessionUser.setCid(1);

        user = new User();
        user.setCid(1);
    }

    // ============ LIKE ============

    @Test
    void likeEvent_WhenNoSession_ShouldReturnUnauthorized() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity response = likeService.likeEvent(1);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void likeEvent_WhenEventNotFound_ShouldReturnNotFound() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity response = likeService.likeEvent(999);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void likeEvent_WhenAlreadyLiked_ShouldReturnBadRequest() {
        Set<User> likers = new HashSet<>();
        likers.add(user);

        Event event = new Event();
        event.setLikers(likers);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity response = likeService.likeEvent(1);

        assertEquals(400, response.getStatusCode().value());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void likeEvent_WhenNotLikedYet_ShouldAddAndSave() {
        Event event = new Event();
        event.setLikers(new HashSet<>());

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity response = likeService.likeEvent(1);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(event.getLikers().contains(user));
        verify(eventRepository).save(event);
    }

    // ============ UNLIKE ============

    @Test
    void unlikeEvent_WhenNotLikedYet_ShouldReturnBadRequest() {
        Event event = new Event();
        event.setLikers(new HashSet<>());

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity response = likeService.unlikeEvent(1);

        assertEquals(400, response.getStatusCode().value());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void unlikeEvent_WhenLiked_ShouldRemoveAndSave() {
        Set<User> likers = new HashSet<>();
        likers.add(user);
        Event event = new Event();
        event.setLikers(likers);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity response = likeService.unlikeEvent(1);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(event.getLikers().isEmpty());
        verify(eventRepository).save(event);
    }

    // ============ COUNT ============

    @Test
    void getEventLikeCount_WhenEventNotFound_ShouldReturnNotFound() {
        when(eventRepository.existsById(999)).thenReturn(false);

        ResponseEntity response = likeService.getEventLikeCount(999);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getEventLikeCount_ShouldReturnCount() {
        when(eventRepository.existsById(1)).thenReturn(true);
        when(eventRepository.countLikesOfEvent(1)).thenReturn(42L);

        ResponseEntity response = likeService.getEventLikeCount(1);

        assertEquals(200, response.getStatusCode().value());
    }

    // ============ MY LIKED ============

    @Test
    void getMyLikedEvents_ShouldReturnPagedResults() {
        Page<Event> mockPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.searchLikedEvents(eq(1), any(), any(), anyString(), any()))
                .thenReturn(mockPage);

        ResponseEntity response = likeService.getMyLikedEvents(
                1, 0, 10, "executionDate", "ASC", "", null);

        assertEquals(200, response.getStatusCode().value());
    }
}