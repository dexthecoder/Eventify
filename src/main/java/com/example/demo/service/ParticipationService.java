package com.example.demo.service;

import com.example.demo.dto.EventResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.entity.Event;
import com.example.demo.entity.Participation;
import com.example.demo.entity.User;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.ParticipationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest request;
    private final ModelMapper model;
    private final EventService eventService;

    @Caching(evict = {
            @CacheEvict(value = "participated_events", allEntries = true),
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true)
    })
    public ResponseEntity joinEvent(Integer eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));

        if (event.getStatus() != EStatus.PUBLISHED) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu etkinlik şu an katılıma açık değil."));
        }

        if (event.getExecutionDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Süresi geçmiş bir etkinliğe katılamazsınız."));
        }

        if (participationRepository.findByUser_CidAndEvent_Id(sessionUser.getCid(), eventId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu etkinliğe zaten katıldınız."));
        }

        User user = userRepository.findById(sessionUser.getCid())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        Participation participation = new Participation();
        participation.setUser(user);
        participation.setEvent(event);
        participationRepository.save(participation);

        return ResponseEntity.ok().body(Map.of("success", true, "message", "Etkinliğe başarıyla katıldınız!"));
    }

    @Caching(evict = {
            @CacheEvict(value = "participated_events", allEntries = true),
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true)
    })
    public ResponseEntity leaveEvent(Integer eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) return ResponseEntity.status(401).body(Map.of("message", "Oturum bulunamadı."));

        Participation participation = participationRepository
                .findByUser_CidAndEvent_Id(sessionUser.getCid(), eventId)
                .orElse(null);

        if (participation == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu etkinliğe zaten katılmamışsınız."));
        }

        participationRepository.delete(participation);
        return ResponseEntity.ok().body(Map.of("success", true, "message", "Etkinlikten ayrıldınız."));
    }

    public ResponseEntity getEventParticipants(Integer eventId) {
        List<Participation> participations = participationRepository.findByEvent_Id(eventId);
        List<UserResponseDto> participants = participations.stream()
                .map(p -> model.map(p.getUser(), UserResponseDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(participants);
    }

    @Caching(evict = {
            @CacheEvict(value = "participated_events", allEntries = true),
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true)
    })
    public ResponseEntity banUserFromEvent(Integer eventId, Integer targetUserId, Integer requesterId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));
        }

        // Sadece etkinlik sahibi banlayabilir
        if (!event.getCreator().getCid().equals(requesterId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Yetkisiz işlem!"));
        }

        // Etkinliği oluşturan kişi kendini banlayamaz
        if (targetUserId.equals(requesterId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kendinizi etkinlikten çıkaramazsınız."));
        }

        Participation participation = participationRepository
                .findByUser_CidAndEvent_Id(targetUserId, eventId)
                .orElse(null);

        if (participation == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu kullanıcı etkinliğe katılmamış."));
        }

        participationRepository.delete(participation);
        return ResponseEntity.ok().body(Map.of("success", true, "message", "Kullanıcı etkinlikten çıkarıldı."));
    }

    @Cacheable(value = "participated_events", key = "{#userId, #page, #size, #sortBy, #direction, #search, #category, #status}")
    public ResponseEntity getMyParticipatedEvents(
            Integer userId, int page, int size, String sortBy, String direction,
            String search, ECategory category, EStatus status) {

        Sort sort = direction.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Event> eventPage = eventRepository.searchParticipatedEventsWithStatus(
                userId, status, category, search, pageable);
        Page<EventResponseDto> responsePage = eventPage.map(eventService::toDto);

        return ResponseEntity.ok().body(responsePage);
    }


}