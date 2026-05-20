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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ModelMapper model;
    private final HttpServletRequest request;
    private final CommentRepository commentRepository;

    private final String UPLOAD_DIR = "uploads/event-images/";
    private final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");

    // Event dto dönüşümü için yardımcı metod
    public EventResponseDto toDto(Event event) {
        EventResponseDto dto = model.map(event, EventResponseDto.class);
        dto.setLikeCount(event.getLikers() != null ? event.getLikers().size() : 0);
        dto.setParticipantCount(event.getParticipations() != null ? event.getParticipations().size() : 0);
        return dto;
    }

    @Caching(evict = {
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true)
    })
    public ResponseEntity createEvent(EventCreateRequestDto eventDto, MultipartFile file) {

        boolean exists = eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                eventDto.getTitle(), eventDto.getCategory(), EStatus.PUBLISHED);

        if (exists) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Bu kategori altında aynı isimle aktif bir etkinlik zaten mevcut."));
        }

        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Oturum bulunamadı."));
        }

        User creator = userRepository.findById(sessionUser.getCid())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));
        Event event = model.map(eventDto, Event.class);
        event.setCreator(creator);
        event.setStatus(EStatus.PUBLISHED);

        try {
            event.setImagePath(handleSecureImageUpload(file, event));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Dosya kaydedilirken teknik bir hata oluştu."));
        }

        Event savedEvent = eventRepository.save(event);
        return ResponseEntity.ok().body(toDto(savedEvent));
    }

    public ResponseEntity getEventById(Integer id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));
        return ResponseEntity.ok().body(toDto(event));
    }

    @Caching(evict = {
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true),
            @CacheEvict(value = "participated_events", allEntries = true),
            @CacheEvict(value = "liked_events", allEntries = true)
    })
    public ResponseEntity deleteEvent(Integer eventId) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Oturum bulunamadı."));
        }

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Etkinlik bulunamadı."));
        }

        if (!event.getCreator().getCid().equals(sessionUser.getCid())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Bu etkinliği silme yetkiniz yok!"));
        }

        // Önce yorumları sil
        commentRepository.deleteAll(
                commentRepository.findByEvent_IdOrderByCreationDateDesc(eventId)
        );

        // Görsel sil
        String imagePath = event.getImagePath();
        if (imagePath != null && !imagePath.startsWith("default_")) {
            try {
                Files.deleteIfExists(Paths.get(UPLOAD_DIR + imagePath));
            } catch (IOException e) {
                System.err.println("Dosya silinirken hata oluştu: " + e.getMessage());
            }
        }

        eventRepository.delete(event);
        return ResponseEntity.ok().body(Map.of("success", true, "message", "Etkinlik ve görseli başarıyla silindi."));
    }

    @Caching(evict = {
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true),
            @CacheEvict(value = "participated_events", allEntries = true),
            @CacheEvict(value = "liked_events", allEntries = true)
    })
    public ResponseEntity updateEvent(Integer eventId, EventUpdateRequestDto updateDto, MultipartFile file) {
        UserResponseDto sessionUser = (UserResponseDto) request.getSession().getAttribute("user");
        Event event = eventRepository.findById(eventId).orElse(null);

        if (event == null) return ResponseEntity.status(404).body(Map.of("message", "Etkinlik bulunamadı."));
        if (!event.getCreator().getCid().equals(sessionUser.getCid())) {
            return ResponseEntity.status(403).body(Map.of("message", "Yetkisiz işlem!"));
        }

        // Arşivlenmiş etkinlik güncellenemez
        if (event.getStatus() == EStatus.ARCHIVED) {
            return ResponseEntity.badRequest().body(Map.of("message", "Arşivlenmiş etkinlik güncellenemez."));
        }

        model.map(updateDto, event);

        if (file != null && !file.isEmpty()) {
            try {
                if (event.getImagePath() != null && !event.getImagePath().startsWith("default_")) {
                    Files.deleteIfExists(Paths.get(UPLOAD_DIR + event.getImagePath()));
                }
                event.setImagePath(handleSecureImageUpload(file, event));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(Map.of("message", "Resim hatası."));
            }
        }

        eventRepository.save(event);
        return ResponseEntity.ok().body(toDto(event));
    }

    @Cacheable(value = "events", key = "{#page, #size, #sortBy, #direction, #search, #category}")
    public ResponseEntity getAllEvents(int page, int size, String sortBy, String direction, String search, ECategory category) {
        Sort sort = direction.equalsIgnoreCase("DESC") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Event> eventPage = eventRepository.searchEvents(EStatus.PUBLISHED, category, search, pageable);
        Page<EventResponseDto> responsePage = eventPage.map(this::toDto);
        return ResponseEntity.ok().body(responsePage);
    }

    @Cacheable(value = "user_events", key = "{#userId, #page, #size, #sortBy, #direction, #search, #category}")
    public ResponseEntity getUsersEvents(Integer userId, int page, int size, String sortBy, String direction, String search, ECategory category) {
        Sort sort = direction.equalsIgnoreCase("DESC") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Event> eventPage = eventRepository.searchUserEvents(userId, category, search, pageable);
        Page<EventResponseDto> responsePage = eventPage.map(this::toDto);
        return ResponseEntity.ok().body(responsePage);
    }

    private String handleSecureImageUpload(MultipartFile file, Event event) throws IOException {
        if (file == null || file.isEmpty()) {
            return switch (event.getCategory()) {
                case MUSIC -> "default_music.jpg";
                case SPORT -> "default_sport.jpg";
                case TECH -> "default_tech.jpg";
                case THEATER -> "default_theater.jpg";
                case WORKSHOP -> "default_workshop.jpg";
                default -> "default_other.jpg";
            };
        }

        if (file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException("Dosya boyutu çok büyük! Maksimum 2MB yükleyebilirsiniz.");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new IllegalArgumentException("Sadece resim dosyaları yüklenebilir.");

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains("."))
            throw new IllegalArgumentException("Geçersiz dosya adı.");



        String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        // Double extension kontrolü (uzantıdan önce başka bir nokta olmamalı)
        String nameWithoutExtension = originalName.substring(0, originalName.lastIndexOf("."));
        if (nameWithoutExtension.contains(".")) {
            throw new IllegalArgumentException("Geçersiz dosya adı.");
        }

        if (!ALLOWED_EXTENSIONS.contains(extension))
            throw new IllegalArgumentException("Sadece JPG, JPEG ve PNG uzantılı dosyalara izin verilir.");

        // Dosya Kayıt İşlemi
        String fileName = UUID.randomUUID().toString() + extension;
        Path path = Paths.get(UPLOAD_DIR + fileName);

        // Klasör yoksa oluştur
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        return fileName;
    }
}