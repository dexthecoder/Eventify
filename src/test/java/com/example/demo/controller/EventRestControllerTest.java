package com.example.demo.controller;

import com.example.demo.dto.EventCreateRequestDto;
import com.example.demo.dto.EventUpdateRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.CommentService;
import com.example.demo.service.EventService;
import com.example.demo.util.ECategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventRestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EventRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private CommentService commentService;

    private MockHttpSession authenticatedSession() {
        UserResponseDto user = new UserResponseDto();
        user.setCid(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    // ============ CREATE ============

    @Test
    public void testCreateEvent_ShouldReturnOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test data".getBytes());

        when(eventService.createEvent(any(EventCreateRequestDto.class), any()))
                .thenReturn(ResponseEntity.ok("Etkinlik oluşturuldu."));

        mockMvc.perform(multipart("/event/create")
                        .file(file)
                        .param("title", "Test Etkinlik")
                        .param("description", "Test açıklama")
                        .param("executionDate", "2030-12-31T20:00:00")
                        .param("location", "Bursa")
                        .param("category", "MUSIC")
                        .session(authenticatedSession()))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateEvent_WhenValidationFails_ShouldReturnBadRequest() throws Exception {
        // Sadece title gönder, diğer required field'lar eksik
        mockMvc.perform(multipart("/event/create")
                        .param("title", "Test")
                        .session(authenticatedSession()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateEvent_WithoutImage_ShouldStillWork() throws Exception {
        // Resim opsiyonel (required = false)
        when(eventService.createEvent(any(EventCreateRequestDto.class), isNull()))
                .thenReturn(ResponseEntity.ok("Default resimle oluşturuldu."));

        mockMvc.perform(multipart("/event/create")
                        .param("title", "Resimsiz Etkinlik")
                        .param("description", "Açıklama")
                        .param("executionDate", "2030-12-31T20:00:00")
                        .param("location", "İstanbul")
                        .param("category", "TECH")
                        .session(authenticatedSession()))
                .andExpect(status().isOk());
    }

    // ============ DELETE ============

    @Test
    public void testDeleteEvent_ShouldReturnOk() throws Exception {
        Integer eventId = 1;
        when(eventService.deleteEvent(eventId))
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Etkinlik silindi.")));

        mockMvc.perform(delete("/event/delete/{id}", eventId)
                        .session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testDeleteEvent_WhenNotFound_ShouldReturnNotFound() throws Exception {
        Integer eventId = 999;
        when(eventService.deleteEvent(eventId))
                .thenReturn(ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı.")));

        mockMvc.perform(delete("/event/delete/{id}", eventId)
                        .session(authenticatedSession()))
                .andExpect(status().isNotFound());
    }

    // ============ UPDATE ============

    @Test
    public void testUpdateEvent_ShouldReturnOk() throws Exception {
        Integer eventId = 1;
        MockMultipartFile file = new MockMultipartFile(
                "image", "updated.jpg", "image/jpeg", "data".getBytes());

        when(eventService.updateEvent(eq(eventId), any(EventUpdateRequestDto.class), any()))
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Güncellendi.")));

        mockMvc.perform(multipart("/event/update/{id}", eventId)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "Güncellenmiş Başlık")
                        .param("description", "Yeni açıklama")
                        .param("executionDate", "2030-12-31T20:00:00")
                        .param("location", "Ankara")
                        .param("category", "SPORT")
                        .session(authenticatedSession()))
                .andExpect(status().isOk());
    }

    // ============ LIST ============

    @Test
    public void testGetAllEvents_ShouldReturnOk() throws Exception {
        when(eventService.getAllEvents(anyInt(), anyInt(), anyString(), anyString(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("Liste"));

        mockMvc.perform(get("/event/list")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "executionDate")
                        .param("direction", "ASC")
                        .param("search", "test")
                        .param("category", "MUSIC"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAllEvents_WithDefaults_ShouldReturnOk() throws Exception {
        when(eventService.getAllEvents(anyInt(), anyInt(), anyString(), anyString(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("Liste"));

        mockMvc.perform(get("/event/list"))
                .andExpect(status().isOk());
    }

    // ============ MY EVENTS ============

    @Test
    public void testGetMyEvents_WhenUserLoggedIn_ShouldReturnOk() throws Exception {
        when(eventService.getUsersEvents(eq(1), anyInt(), anyInt(), anyString(), anyString(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("Kullanıcı etkinlikleri"));

        mockMvc.perform(get("/event/my-events")
                        .session(authenticatedSession()))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetMyEvents_WhenUserNotLoggedIn_ShouldReturnUnauthorized() throws Exception {
        // Session yok
        mockMvc.perform(get("/event/my-events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Oturum bulunamadı."));
    }
}