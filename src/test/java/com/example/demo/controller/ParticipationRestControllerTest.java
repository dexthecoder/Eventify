package com.example.demo.controller;

import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.ParticipationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParticipationRestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ParticipationRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipationService participationService;

    private MockHttpSession authenticatedSession() {
        UserResponseDto user = new UserResponseDto();
        user.setCid(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    // ============ JOIN ============

    @Test
    public void testJoinEvent_ShouldReturnOk() throws Exception {
        when(participationService.joinEvent(1))
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Katıldınız!")));

        mockMvc.perform(post("/participation/{eventId}/join", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testJoinEvent_WhenAlreadyJoined_ShouldReturnBadRequest() throws Exception {
        when(participationService.joinEvent(1))
                .thenReturn(ResponseEntity.badRequest()
                        .body(Map.of("message", "Bu etkinliğe zaten katıldınız.")));

        mockMvc.perform(post("/participation/{eventId}/join", 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJoinEvent_WhenEventExpired_ShouldReturnBadRequest() throws Exception {
        when(participationService.joinEvent(1))
                .thenReturn(ResponseEntity.badRequest()
                        .body(Map.of("message", "Süresi geçmiş bir etkinliğe katılamazsınız.")));

        mockMvc.perform(post("/participation/{eventId}/join", 1))
                .andExpect(status().isBadRequest());
    }

    // ============ LEAVE ============

    @Test
    public void testLeaveEvent_ShouldReturnOk() throws Exception {
        when(participationService.leaveEvent(1))
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Ayrıldınız.")));

        mockMvc.perform(delete("/participation/{eventId}/leave", 1))
                .andExpect(status().isOk());
    }

    @Test
    public void testLeaveEvent_WhenNotJoined_ShouldReturnBadRequest() throws Exception {
        when(participationService.leaveEvent(1))
                .thenReturn(ResponseEntity.badRequest()
                        .body(Map.of("message", "Bu etkinliğe zaten katılmamışsınız.")));

        mockMvc.perform(delete("/participation/{eventId}/leave", 1))
                .andExpect(status().isBadRequest());
    }

    // ============ PARTICIPANTS ============

    @Test
    public void testGetEventParticipants_ShouldReturnOk() throws Exception {
        when(participationService.getEventParticipants(1))
                .thenReturn(ResponseEntity.ok(List.of(new UserResponseDto())));

        mockMvc.perform(get("/participation/{eventId}/users", 1))
                .andExpect(status().isOk());
    }

    // ============ MY PARTICIPATIONS ============

    @Test
    public void testGetMyParticipatedEvents_WhenLoggedIn_ShouldReturnOk() throws Exception {
        when(participationService.getMyParticipatedEvents(
                eq(1), anyInt(), anyInt(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(ResponseEntity.ok("Katıldığım etkinlikler"));

        mockMvc.perform(get("/participation/my-participations")
                        .session(authenticatedSession()))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetMyParticipatedEvents_WhenNotLoggedIn_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/participation/my-participations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Oturum bulunamadı."));
    }
}