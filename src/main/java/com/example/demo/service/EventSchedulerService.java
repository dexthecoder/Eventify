package com.example.demo.service;

import com.example.demo.entity.Event;
import com.example.demo.repository.EventRepository;
import com.example.demo.util.EStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSchedulerService {

    private final EventRepository eventRepository;
    private static final Logger logger = LoggerFactory.getLogger(EventSchedulerService.class);

    @Scheduled(cron = "0 0 * * * *") // Her saat başı çalışır
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "user_events", allEntries = true),
            @CacheEvict(value = "participated_events", allEntries = true),
            @CacheEvict(value = "liked_events", allEntries = true)
    })
    public void archiveExpiredEvents() {
        LocalDateTime now = LocalDateTime.now();

        List<Event> expiredEvents = eventRepository.findExpiredActiveEvents(now);

        if (expiredEvents.isEmpty()) return;

        expiredEvents.forEach(event -> event.setStatus(EStatus.ARCHIVED));
        eventRepository.saveAll(expiredEvents);

        logger.info("Otomatik arşivleme: {} etkinlik arşivlendi.", expiredEvents.size());
    }
}