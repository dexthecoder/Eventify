package com.example.demo.repository;

import com.example.demo.entity.Comment;
import com.example.demo.entity.Event;
import com.example.demo.entity.User;
import com.example.demo.util.ECategory;
import com.example.demo.util.EStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager em;

    private Event event;
    private Event otherEvent;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Polat");
        user.setSurname("Test");
        user.setEmail("p@t.com");
        user.setPhone("05551234567");
        user.setPassword("pw");
        user.setEnabled(true);
        em.persist(user);

        event = createEvent("Etkinlik 1");
        otherEvent = createEvent("Etkinlik 2");
        em.persist(event);
        em.persist(otherEvent);

        // event'e 3 yorum (manuel olarak tarih set ediyoruz - Thread.sleep flaky olur)
        em.persist(createComment("Yorum 1", event, LocalDateTime.now().minusHours(3)));
        em.persist(createComment("Yorum 2", event, LocalDateTime.now().minusHours(2)));
        em.persist(createComment("Yorum 3", event, LocalDateTime.now().minusHours(1)));

        // otherEvent'e 1 yorum (karıştığını anlamak için)
        em.persist(createComment("Başka etkinlik yorumu", otherEvent, LocalDateTime.now()));

        em.flush();
    }

    private Event createEvent(String title) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription("d");
        e.setLocation("l");
        e.setCategory(ECategory.MUSIC);
        e.setStatus(EStatus.PUBLISHED);
        e.setExecutionDate(LocalDateTime.now().plusDays(5));
        e.setCreator(user);
        return e;
    }

    private Comment createComment(String text, Event ev, LocalDateTime createdAt) {
        Comment c = new Comment();
        c.setText(text);
        c.setEvent(ev);
        c.setCreator(user);
        c.setCreationDate(createdAt);
        return c;
    }

    // ============ findByEvent_IdOrderByCreationDateDesc ============

    @Test
    void findByEvent_ShouldReturnOnlyMatchingEventComments() {
        List<Comment> comments = commentRepository
                .findByEvent_IdOrderByCreationDateDesc(event.getId());

        assertEquals(3, comments.size());
        // Diğer etkinliğin yorumu sızmamalı
        boolean hasOtherEventComment = comments.stream()
                .anyMatch(c -> c.getText().equals("Başka etkinlik yorumu"));
        assertFalse(hasOtherEventComment);
    }

    @Test
    void findByEvent_ShouldOrderByCreationDateDesc() {
        List<Comment> comments = commentRepository
                .findByEvent_IdOrderByCreationDateDesc(event.getId());

        assertEquals(3, comments.size());

        // Tarihlerin azalan sırada olduğunu kontrol et
        for (int i = 0; i < comments.size() - 1; i++) {
            assertTrue(
                    comments.get(i).getCreationDate()
                            .isAfter(comments.get(i + 1).getCreationDate()) ||
                            comments.get(i).getCreationDate()
                                    .isEqual(comments.get(i + 1).getCreationDate())
            );
        }
    }

    @Test
    void findByEvent_WhenNoComments_ShouldReturnEmpty() {
        List<Comment> comments = commentRepository
                .findByEvent_IdOrderByCreationDateDesc(99999);

        assertTrue(comments.isEmpty());
    }
}