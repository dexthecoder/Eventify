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
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ModelMapper model;
    private final HttpServletRequest request;

    public ResponseEntity addComment(Integer eventId, CommentRequestDto dto) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));

        User creator = userRepository.findById(sessionUser.getCid())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setEvent(event);
        comment.setCreator(creator);

        Comment savedComment = commentRepository.save(comment);
        return ResponseEntity.ok().body(model.map(savedComment, CommentResponseDto.class));
    }

    // yorum güncelleme (Sadece yorum sahibi)
    public ResponseEntity updateComment(Integer commentId, CommentRequestDto dto) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return ResponseEntity.status(404).body(Map.of("message", "Yorum bulunamadı."));

        // Yorumun sahibi mi?
        if (!comment.getCreator().getCid().equals(sessionUser.getCid())) {
            return ResponseEntity.status(403).body(Map.of("message", "Sadece kendi yorumunuzu düzenleyebilirsiniz!"));
        }

        comment.setText(dto.getText());
        commentRepository.save(comment);

        return ResponseEntity.ok().body(model.map(comment, CommentResponseDto.class));
    }

    // yorum silme (yorum sahibi veya etkinlik sahibi)
    public ResponseEntity deleteComment(Integer commentId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return ResponseEntity.status(404).body(Map.of("message", "Yorum bulunamadı."));

        Integer currentUserId = sessionUser.getCid();
        Integer commentOwnerId = comment.getCreator().getCid();
        Integer eventOwnerId = comment.getEvent().getCreator().getCid();

        // Yetki kontrolü: yorum sahibi mi veya etkinlik sahibi mi?
        boolean isCommentOwner = currentUserId.equals(commentOwnerId);
        boolean isEventOwner = currentUserId.equals(eventOwnerId);

        if (!isCommentOwner && !isEventOwner) {
            return ResponseEntity.status(403).body(Map.of("message", "Bu yorumu silme yetkiniz yok!"));
        }

        commentRepository.delete(comment);
        return ResponseEntity.ok().body(Map.of("success", true, "message", "Yorum başarıyla silindi."));
    }

    // etkinlğin yorumlarını listele
    public ResponseEntity getCommentsOfEvent(Integer eventId) {
        // Repositoryden etkinliğe ait yorumları sırala (tarihe göre)
        java.util.List<Comment> comments = commentRepository.findByEvent_IdOrderByCreationDateDesc(eventId);

        java.util.List<CommentResponseDto> responseList = comments.stream()
                .map(comment -> model.map(comment, CommentResponseDto.class))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok().body(responseList);
    }
}