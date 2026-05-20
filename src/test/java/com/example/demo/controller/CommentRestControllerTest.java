package com.example.demo.controller;

import com.example.demo.dto.CommentRequestDto;
import com.example.demo.dto.CommentResponseDto;
import com.example.demo.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentRestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CommentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    // ============ ADD COMMENT ============

    @Test
    public void testAddComment_ShouldReturnOk() throws Exception {
        Integer eventId = 1;
        CommentRequestDto dto = new CommentRequestDto();
        dto.setText("Harika bir etkinlik!");

        CommentResponseDto response = new CommentResponseDto();
        response.setId(1);
        response.setText("Harika bir etkinlik!");

        when(commentService.addComment(eq(eventId), any(CommentRequestDto.class)))
                .thenReturn(ResponseEntity.ok(response));

        mockMvc.perform(post("/comment/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Harika bir etkinlik!"));
    }

    @Test
    public void testAddComment_WhenTextBlank_ShouldReturnBadRequest() throws Exception {
        CommentRequestDto dto = new CommentRequestDto();
        dto.setText(""); // boş

        mockMvc.perform(post("/comment/{eventId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddComment_WhenEventNotFound_ShouldReturnNotFound() throws Exception {
        CommentRequestDto dto = new CommentRequestDto();
        dto.setText("Yorum");

        when(commentService.addComment(eq(999), any(CommentRequestDto.class)))
                .thenReturn(ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı.")));

        mockMvc.perform(post("/comment/{eventId}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ============ GET COMMENTS ============

    @Test
    public void testGetEventComments_ShouldReturnOk() throws Exception {
        when(commentService.getCommentsOfEvent(1))
                .thenReturn(ResponseEntity.ok(List.of(new CommentResponseDto())));

        mockMvc.perform(get("/comment/{eventId}", 1))
                .andExpect(status().isOk());
    }

    // ============ UPDATE COMMENT ============

    @Test
    public void testUpdateComment_ShouldReturnOk() throws Exception {
        CommentRequestDto dto = new CommentRequestDto();
        dto.setText("Güncellenmiş yorum");

        when(commentService.updateComment(eq(1), any(CommentRequestDto.class)))
                .thenReturn(ResponseEntity.ok(new CommentResponseDto()));

        mockMvc.perform(put("/comment/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateComment_WhenNotOwner_ShouldReturnForbidden() throws Exception {
        CommentRequestDto dto = new CommentRequestDto();
        dto.setText("Yorum");

        when(commentService.updateComment(eq(1), any(CommentRequestDto.class)))
                .thenReturn(ResponseEntity.status(403)
                        .body(Map.of("message", "Sadece kendi yorumunuzu düzenleyebilirsiniz!")));

        mockMvc.perform(put("/comment/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ============ DELETE COMMENT ============

    @Test
    public void testDeleteComment_ShouldReturnOk() throws Exception {
        when(commentService.deleteComment(1))
                .thenReturn(ResponseEntity.ok(Map.of("success", true, "message", "Yorum silindi.")));

        mockMvc.perform(delete("/comment/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testDeleteComment_WhenNotAuthorized_ShouldReturnForbidden() throws Exception {
        when(commentService.deleteComment(1))
                .thenReturn(ResponseEntity.status(403)
                        .body(Map.of("message", "Bu yorumu silme yetkiniz yok!")));

        mockMvc.perform(delete("/comment/{id}", 1))
                .andExpect(status().isForbidden());
    }
}