package com.example.demo.repository;

import com.example.demo.entity.Event;
import com.example.demo.entity.User;
import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestEntityManager em;

    private User creator;
    private User otherCreator;

    @BeforeEach
    void setUp() {
        creator = createUser("polat@test.com", "05551111111");
        otherCreator = createUser("other@test.com", "05552222222");
        em.persist(creator);
        em.persist(otherCreator);

        em.persist(createEvent("Konser", "Müzik etkinliği", ECategory.MUSIC, EStatus.PUBLISHED, creator));
        em.persist(createEvent("Maç", "Spor etkinliği", ECategory.SPORT, EStatus.PUBLISHED, creator));
        em.persist(createEvent("Workshop", "Yazılım eğitimi", ECategory.TECH, EStatus.UNPUBLISHED, creator));
        em.persist(createEvent("Eski Konser", "Arşiv", ECategory.MUSIC, EStatus.ARCHIVED, creator));
        em.persist(createEvent("Başkasının Etkinliği", "Tiyatro", ECategory.THEATER, EStatus.PUBLISHED, otherCreator));
        em.flush();
    }

    private User createUser(String email, String phone) {
        User u = new User();
        u.setName("Test");
        u.setSurname("User");
        u.setEmail(email);
        u.setPhone(phone);
        u.setPassword("pw");
        u.setEnabled(true);
        return u;
    }

    private Event createEvent(String title, String desc, ECategory cat, EStatus status, User who) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription(desc);
        e.setLocation("Bursa");
        e.setCategory(cat);
        e.setStatus(status);
        e.setExecutionDate(LocalDateTime.now().plusDays(5));
        e.setCreator(who);
        return e;
    }

    // ============ existsByTitle (benzersizlik kontrolü) ============

    @Test
    void existsByTitle_WhenMatchingPublished_ShouldReturnTrue() {
        boolean exists = eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                "Konser", ECategory.MUSIC, EStatus.PUBLISHED);

        assertTrue(exists);
    }

    @Test
    void existsByTitle_CaseInsensitive() {
        boolean exists = eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                "KONSER", ECategory.MUSIC, EStatus.PUBLISHED);

        assertTrue(exists);
    }

    @Test
    void existsByTitle_WhenDifferentCategory_ShouldReturnFalse() {
        boolean exists = eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                "Konser", ECategory.SPORT, EStatus.PUBLISHED);

        assertFalse(exists);
    }

    @Test
    void existsByTitle_WhenUnpublishedStatus_ShouldReturnFalse() {
        boolean exists = eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                "Workshop", ECategory.TECH, EStatus.PUBLISHED);

        assertFalse(exists);
    }

    @Test
    void existsByTitle_WhenArchivedStatus_ShouldReturnFalse() {
        // Aynı isimli arşivlenmiş etkinlik var ama PUBLISHED arandığı için false dönmeli
        boolean exists = eventRepository.existsByTitleIgnoreCaseAndCategoryAndStatus(
                "Eski Konser", ECategory.MUSIC, EStatus.PUBLISHED);

        assertFalse(exists);
    }

    // ============ searchEvents ============

    @Test
    void searchEvents_WhenCategoryNull_ShouldReturnAllPublished() {
        Page<Event> result = eventRepository.searchEvents(
                EStatus.PUBLISHED, null, "", PageRequest.of(0, 10));

        // 3 PUBLISHED: Konser, Maç, Başkasının Etkinliği
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void searchEvents_WithCategoryFilter_ShouldReturnOnlyMatching() {
        Page<Event> result = eventRepository.searchEvents(
                EStatus.PUBLISHED, ECategory.MUSIC, "", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Konser", result.getContent().get(0).getTitle());
    }

    @Test
    void searchEvents_WithSearchTerm_ShouldMatchTitle() {
        Page<Event> result = eventRepository.searchEvents(
                EStatus.PUBLISHED, null, "Konser", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchEvents_WithSearchTerm_ShouldMatchDescription() {
        Page<Event> result = eventRepository.searchEvents(
                EStatus.PUBLISHED, null, "müzik", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchEvents_SearchCaseInsensitive() {
        Page<Event> result = eventRepository.searchEvents(
                EStatus.PUBLISHED, null, "KONSER", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchEvents_ShouldExcludeNonPublishedEvents() {
        // UNPUBLISHED ve ARCHIVED olanlar listeye dahil olmamalı
        Page<Event> result = eventRepository.searchEvents(
                EStatus.PUBLISHED, null, "", PageRequest.of(0, 10));

        boolean hasUnpublished = result.getContent().stream()
                .anyMatch(e -> e.getStatus() != EStatus.PUBLISHED);
        assertFalse(hasUnpublished);
    }

    @Test
    void searchEvents_Pagination_ShouldWork() {
        Page<Event> firstPage = eventRepository.searchEvents(
                EStatus.PUBLISHED, null, "", PageRequest.of(0, 2));

        assertEquals(2, firstPage.getContent().size());
        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
    }

    // ============ searchUserEvents ============

    @Test
    void searchUserEvents_ShouldReturnOnlyOwnerEvents() {
        Page<Event> result = eventRepository.searchUserEvents(
                creator.getCid(), null, "", PageRequest.of(0, 10));

        // creator'ın 4 etkinliği var (her statüden)
        assertEquals(4, result.getTotalElements());
    }

    @Test
    void searchUserEvents_WhenOtherUser_ShouldReturnOnlyTheirs() {
        Page<Event> result = eventRepository.searchUserEvents(
                otherCreator.getCid(), null, "", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Başkasının Etkinliği", result.getContent().get(0).getTitle());
    }

    @Test
    void searchUserEvents_WhenNonExistentUser_ShouldReturnEmpty() {
        Page<Event> result = eventRepository.searchUserEvents(
                9999, null, "", PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void searchUserEvents_WithCategoryFilter() {
        Page<Event> result = eventRepository.searchUserEvents(
                creator.getCid(), ECategory.MUSIC, "", PageRequest.of(0, 10));

        // creator'ın MUSIC kategorisinde 2 etkinliği var (Konser PUBLISHED + Eski Konser ARCHIVED)
        assertEquals(2, result.getTotalElements());
    }

    // ============ countLikes ============

    @Test
    void countLikesOfEvent_WhenNoLikes_ShouldReturnZero() {
        Event firstEvent = em.getEntityManager()
                .createQuery("SELECT e FROM Event e WHERE e.title = 'Konser'", Event.class)
                .getSingleResult();

        long count = eventRepository.countLikesOfEvent(firstEvent.getId());

        assertEquals(0L, count);
    }
}