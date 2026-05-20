package com.example.demo.service;

import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.Event;
import com.example.demo.entity.Participation;
import com.example.demo.entity.User;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ParticipationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.EStatus;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock private ParticipationRepository participationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpSession session;
    @Mock private EventService eventService;
    @Mock private ModelMapper model;

    @InjectMocks
    private ParticipationService participationService;

    private UserResponseDto sessionUser;
    private User user;

    @BeforeEach
    void setUp() {
        sessionUser = new UserResponseDto();
        sessionUser.setCid(1);

        user = new User();
        user.setCid(1);
    }

    // ============ JOIN ============

    @Test
    void joinEvent_WhenNoSession_ShouldReturnUnauthorized() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity response = participationService.joinEvent(1);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void joinEvent_WhenEventNotFound_ShouldReturnNotFound() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity response = participationService.joinEvent(999);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void joinEvent_WhenEventNotPublished_ShouldReturnBadRequest() {
        Event event = new Event();
        event.setStatus(EStatus.UNPUBLISHED); // PUBLISHED değil

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        ResponseEntity response = participationService.joinEvent(1);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void joinEvent_WhenEventArchived_ShouldReturnBadRequest() {
        Event event = new Event();
        event.setStatus(EStatus.ARCHIVED); // arşivlenmiş etkinliğe katılım yok

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        ResponseEntity response = participationService.joinEvent(1);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void joinEvent_WhenEventExpired_ShouldReturnBadRequest() {
        Event event = new Event();
        event.setStatus(EStatus.PUBLISHED);
        event.setExecutionDate(LocalDateTime.now().minusDays(1)); // geçmiş tarih

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        ResponseEntity response = participationService.joinEvent(1);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void joinEvent_WhenAlreadyJoined_ShouldReturnBadRequest() {
        Event event = new Event();
        event.setStatus(EStatus.PUBLISHED);
        event.setExecutionDate(LocalDateTime.now().plusDays(1));

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(participationRepository.findByUser_CidAndEvent_Id(1, 1))
                .thenReturn(Optional.of(new Participation()));

        ResponseEntity response = participationService.joinEvent(1);

        assertEquals(400, response.getStatusCode().value());
        verify(participationRepository, never()).save(any());
    }

    @Test
    void joinEvent_WhenValid_ShouldSaveParticipation() {
        Event event = new Event();
        event.setStatus(EStatus.PUBLISHED);
        event.setExecutionDate(LocalDateTime.now().plusDays(1));

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(participationRepository.findByUser_CidAndEvent_Id(1, 1))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        ResponseEntity response = participationService.joinEvent(1);

        assertEquals(200, response.getStatusCode().value());
        verify(participationRepository).save(any(Participation.class));
    }

    // ============ LEAVE ============

    @Test
    void leaveEvent_WhenNoSession_ShouldReturnUnauthorized() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity response = participationService.leaveEvent(1);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void leaveEvent_WhenNotJoined_ShouldReturnBadRequest() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(participationRepository.findByUser_CidAndEvent_Id(1, 1))
                .thenReturn(Optional.empty());

        ResponseEntity response = participationService.leaveEvent(1);

        assertEquals(400, response.getStatusCode().value());
        verify(participationRepository, never()).delete(any());
    }

    @Test
    void leaveEvent_WhenJoined_ShouldDelete() {
        Participation participation = new Participation();

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(participationRepository.findByUser_CidAndEvent_Id(1, 1))
                .thenReturn(Optional.of(participation));

        ResponseEntity response = participationService.leaveEvent(1);

        assertEquals(200, response.getStatusCode().value());
        verify(participationRepository).delete(participation);
    }

    // ============ PARTICIPANTS ============

    @Test
    void getEventParticipants_ShouldReturnList() {
        when(participationRepository.findByEvent_Id(1))
                .thenReturn(Collections.emptyList());

        ResponseEntity response = participationService.getEventParticipants(1);

        assertEquals(200, response.getStatusCode().value());
    }

    // ============ MY PARTICIPATED ============

    @Test
    void getMyParticipatedEvents_ShouldReturnPagedResults() {
        Page<Event> mockPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.searchParticipatedEventsWithStatus(eq(1), any(), any(), anyString(), any()))
                .thenReturn(mockPage);

        ResponseEntity response = participationService.getMyParticipatedEvents(
                1, 0, 10, "executionDate", "DESC", "", null, null);

        assertEquals(200, response.getStatusCode().value());
    }
}