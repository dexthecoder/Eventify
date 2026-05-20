package com.example.demo.service;

import com.example.demo.dto.EventResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.Event;
import com.example.demo.entity.User;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final HttpServletRequest request;

    @Caching(evict = {
            @CacheEvict(value = "liked_events", allEntries = true),
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true)
    })
    public ResponseEntity likeEvent(Integer eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));

        User user = userRepository.findById(sessionUser.getCid())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        if (event.getLikers().contains(user)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu etkinliği zaten beğendiniz."));
        }

        event.getLikers().add(user);
        eventRepository.save(event);

        return ResponseEntity.ok().body(Map.of("success", true, "message", "Etkinlik beğenildi."));
    }

    @Caching(evict = {
            @CacheEvict(value = "liked_events", allEntries = true),
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true)
    })
    public ResponseEntity unlikeEvent(Integer eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));

        // addComment metodunda
        User user = userRepository.findById(sessionUser.getCid())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        if (!event.getLikers().contains(user)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu etkinliği zaten beğenmemişsiniz."));
        }

        event.getLikers().remove(user);
        eventRepository.save(event);

        return ResponseEntity.ok().body(Map.of("success", true, "message", "Beğeni geri çekildi."));
    }

    @Cacheable(value = "liked_events", key = "{#userId, #page, #size, #sortBy, #direction, #search, #category}")
    public ResponseEntity getMyLikedEvents(
            Integer userId, int page, int size, String sortBy, String direction, String search, ECategory category) {

        Sort sort = direction.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Event> eventPage = eventRepository.searchLikedEvents(userId, EStatus.PUBLISHED, category, search, pageable);
        Page<EventResponseDto> responsePage = eventPage.map(eventService::toDto);

        return ResponseEntity.ok().body(responsePage);
    }

    public ResponseEntity getEventLikeCount(Integer eventId) {
        if (!eventRepository.existsById(eventId)) {
            return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));
        }
        long likeCount = eventRepository.countLikesOfEvent(eventId);
        return ResponseEntity.ok().body(Map.of("eventId", eventId, "likeCount", likeCount));
    }
}