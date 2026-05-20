package com.example.demo.service;

import com.example.demo.dto.EventCreateRequestDto;
import com.example.demo.dto.EventResponseDto;
import com.example.demo.dto.EventUpdateRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.Event;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.ECategory;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ModelMapper model;
    @Mock private HttpServletRequest request;
    @Mock private CommentRepository commentRepository; // EKLE
    @Mock private HttpSession session;

    @InjectMocks
    private EventService eventService;

    private EventCreateRequestDto createDto;
    private UserResponseDto sessionUser;
    private User user;

    @BeforeEach
    void setUp() {
        createDto = new EventCreateRequestDto();
        createDto.setTitle("Test Etkinlik");
        createDto.setDescription("Açıklama");
        createDto.setLocation("Bursa");
        createDto.setCategory(ECategory.MUSIC);
        createDto.setExecutionDate(LocalDateTime.now().plusDays(10));

        sessionUser = new UserResponseDto();
        sessionUser.setCid(1);

        user = new User();
        user.setCid(1);
    }

    // ============ CREATE ============

    @Test
    void createEvent_WhenDuplicateExists_ShouldReturnBadRequest() {
        when(eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                "Test Etkinlik", ECategory.MUSIC, EStatus.PUBLISHED))
                .thenReturn(true);

        ResponseEntity response = eventService.createEvent(createDto, null);

        assertEquals(400, response.getStatusCode().value());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_WhenNoSession_ShouldReturnUnauthorized() {
        when(eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                anyString(), any(), any())).thenReturn(false);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity response = eventService.createEvent(createDto, null);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void createEvent_WhenNoImage_ShouldAssignDefaultByCategory() {
        when(eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                anyString(), any(), any())).thenReturn(false);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event mappedEvent = new Event();
        mappedEvent.setCategory(ECategory.MUSIC);
        when(model.map(createDto, Event.class)).thenReturn(mappedEvent);

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(model.map(savedEvent, EventResponseDto.class)).thenReturn(new EventResponseDto());

        ResponseEntity response = eventService.createEvent(createDto, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("default_music.jpg", mappedEvent.getImagePath());
        assertEquals(EStatus.PUBLISHED, mappedEvent.getStatus());
    }

    @Test
    void createEvent_WhenFileTooLarge_ShouldReturnBadRequest() {
        byte[] bigData = new byte[3 * 1024 * 1024]; // 3MB
        MultipartFile bigFile = new MockMultipartFile(
                "image", "big.jpg", "image/jpeg", bigData);

        when(eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                anyString(), any(), any())).thenReturn(false);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event mappedEvent = new Event();
        mappedEvent.setCategory(ECategory.MUSIC);
        when(model.map(createDto, Event.class)).thenReturn(mappedEvent);

        ResponseEntity response = eventService.createEvent(createDto, bigFile);

        assertEquals(400, response.getStatusCode().value());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_WhenInvalidExtension_ShouldReturnBadRequest() {
        MultipartFile badFile = new MockMultipartFile(
                "image", "virus.exe", "image/jpeg", "data".getBytes());

        when(eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                anyString(), any(), any())).thenReturn(false);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event mappedEvent = new Event();
        mappedEvent.setCategory(ECategory.MUSIC);
        when(model.map(createDto, Event.class)).thenReturn(mappedEvent);

        ResponseEntity response = eventService.createEvent(createDto, badFile);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void createEvent_WhenWrongMimeType_ShouldReturnBadRequest() {
        MultipartFile badFile = new MockMultipartFile(
                "image", "doc.jpg", "application/pdf", "data".getBytes());

        when(eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                anyString(), any(), any())).thenReturn(false);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event mappedEvent = new Event();
        mappedEvent.setCategory(ECategory.MUSIC);
        when(model.map(createDto, Event.class)).thenReturn(mappedEvent);

        ResponseEntity response = eventService.createEvent(createDto, badFile);

        assertEquals(400, response.getStatusCode().value());
    }

    // ============ DELETE ============

    @Test
    void deleteEvent_WhenNoSession_ShouldReturnUnauthorized() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity response = eventService.deleteEvent(1);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void deleteEvent_WhenEventNotFound_ShouldReturnNotFound() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity response = eventService.deleteEvent(999);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteEvent_WhenNotOwner_ShouldReturnForbidden() {
        User otherUser = new User();
        otherUser.setCid(999); // farklı kullanıcı

        Event event = new Event();
        event.setCreator(otherUser);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        ResponseEntity response = eventService.deleteEvent(1);

        assertEquals(403, response.getStatusCode().value());
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEvent_WhenOwnerAndDefaultImage_ShouldDeleteWithoutTouchingFile() {
        Event event = new Event();
        event.setCreator(user);
        event.setImagePath("default_music.jpg");

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));
        when(commentRepository.findByEvent_IdOrderByCreationDateDesc(1))
                .thenReturn(List.of()); // yorum yok

        ResponseEntity response = eventService.deleteEvent(1);

        assertEquals(200, response.getStatusCode().value());
        verify(eventRepository).delete(event);
    }

    // ============ UPDATE ============

    @Test
    void updateEvent_WhenEventNotFound_ShouldReturnNotFound() {
        // Service önce session'a, sonra event'e bakıyor — session'ı da stub'lamak gerek
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        ResponseEntity response = eventService.updateEvent(999, new EventUpdateRequestDto(), null);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateEvent_WhenNotOwner_ShouldReturnForbidden() {
        User otherUser = new User();
        otherUser.setCid(999);

        Event event = new Event();
        event.setCreator(otherUser);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(sessionUser);
        when(eventRepository.findById(1)).thenReturn(Optional.of(event));

        ResponseEntity response = eventService.updateEvent(1, new EventUpdateRequestDto(), null);

        assertEquals(403, response.getStatusCode().value());
    }

    // ============ LIST ============

    @Test
    void getAllEvents_ShouldReturnPagedResults() {
        Page<Event> mockPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.searchEvents(eq(EStatus.PUBLISHED), eq(ECategory.MUSIC),
                eq("test"), any())).thenReturn(mockPage);

        ResponseEntity response = eventService.getAllEvents(
                0, 10, "executionDate", "ASC", "test", ECategory.MUSIC);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getUsersEvents_ShouldReturnPagedResults() {
        Page<Event> mockPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.searchUserEvents(eq(1), any(), anyString(), any()))
                .thenReturn(mockPage);

        ResponseEntity response = eventService.getUsersEvents(
                1, 0, 10, "creationDate", "DESC", "", null);

        assertEquals(200, response.getStatusCode().value());
    }
}