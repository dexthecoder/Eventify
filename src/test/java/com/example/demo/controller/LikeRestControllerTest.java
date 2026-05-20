package com.example.demo.controller;

import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.LikeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeRestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class LikeRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LikeService likeService;

    private MockHttpSession authenticatedSession() {
        UserResponseDto user = new UserResponseDto();
        user.setCid(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", user);
        return session;
    }

    // ============ LIKE ============

    @Test
    public void testLikeEvent_ShouldReturnOk() throws Exception {
        when(likeService.likeEvent(1))
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Etkinlik beğenildi.")));

        mockMvc.perform(post("/like/{eventId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testLikeEvent_WhenAlreadyLiked_ShouldReturnBadRequest() throws Exception {
        when(likeService.likeEvent(1))
                .thenReturn(ResponseEntity.badRequest()
                        .body(Map.of("message", "Bu etkinliği zaten beğendiniz.")));

        mockMvc.perform(post("/like/{eventId}", 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLikeEvent_WhenEventNotFound_ShouldReturnNotFound() throws Exception {
        when(likeService.likeEvent(999))
                .thenReturn(ResponseEntity.status(404)
                        .body(Map.of("message", "Etkinlik bulunamadı.")));

        mockMvc.perform(post("/like/{eventId}", 999))
                .andExpect(status().isNotFound());
    }

    // ============ UNLIKE ============

    @Test
    public void testUnlikeEvent_ShouldReturnOk() throws Exception {
        when(likeService.unlikeEvent(1))
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Beğeni geri çekildi.")));

        mockMvc.perform(delete("/like/{eventId}", 1))
                .andExpect(status().isOk());
    }

    @Test
    public void testUnlikeEvent_WhenNotLikedYet_ShouldReturnBadRequest() throws Exception {
        when(likeService.unlikeEvent(1))
                .thenReturn(ResponseEntity.badRequest()
                        .body(Map.of("message", "Bu etkinliği zaten beğenmemişsiniz.")));

        mockMvc.perform(delete("/like/{eventId}", 1))
                .andExpect(status().isBadRequest());
    }

    // ============ COUNT ============

    @Test
    public void testGetEventLikeCount_ShouldReturnOk() throws Exception {
        when(likeService.getEventLikeCount(1))
                .thenReturn(ResponseEntity.ok(Map.of("eventId", 1, "likeCount", 42L)));

        mockMvc.perform(get("/like/count/{eventId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(42));
    }

    // ============ MY LIKES ============

    @Test
    public void testGetMyLikedEvents_WhenLoggedIn_ShouldReturnOk() throws Exception {
        when(likeService.getMyLikedEvents(eq(1), anyInt(), anyInt(), anyString(), anyString(), anyString(), any()))
                .thenReturn(ResponseEntity.ok("Beğenilen etkinlikler"));

        mockMvc.perform(get("/like/my-likes")
                        .session(authenticatedSession()))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetMyLikedEvents_WhenNotLoggedIn_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/like/my-likes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Oturum bulunamadı."));
    }
}