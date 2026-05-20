package com.example.demo.repository;

import com.example.demo.entity.Event;
import com.example.demo.entity.Participation;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ParticipationRepositoryTest {

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private User otherUser;
    private Event event;
    private Event otherEvent;

    @BeforeEach
    void setUp() {
        user = createUser("u1@test.com", "05551111111");
        otherUser = createUser("u2@test.com", "05552222222");
        em.persist(user);
        em.persist(otherUser);

        event = createEvent("Etkinlik 1");
        otherEvent = createEvent("Etkinlik 2");
        em.persist(event);
        em.persist(otherEvent);

        // user → event'e katıldı
        em.persist(createParticipation(user, event));
        // otherUser → event'e de katıldı (aynı event'in 2 katılımcısı)
        em.persist(createParticipation(otherUser, event));
        // user → otherEvent'e de katıldı (user'ın 2 katılımı)
        em.persist(createParticipation(user, otherEvent));

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

    private Participation createParticipation(User u, Event ev) {
        Participation p = new Participation();
        p.setUser(u);
        p.setEvent(ev);
        return p;
    }

    // ============ findByEvent_Id ============

    @Test
    void findByEventId_ShouldReturnAllParticipantsOfEvent() {
        List<Participation> result = participationRepository.findByEvent_Id(event.getId());

        assertEquals(2, result.size()); // user + otherUser
    }

    @Test
    void findByEventId_WhenNoParticipants_ShouldReturnEmpty() {
        List<Participation> result = participationRepository.findByEvent_Id(99999);

        assertTrue(result.isEmpty());
    }

    // ============ findByUser_Cid ============

    @Test
    void findByUserCid_ShouldReturnAllUserParticipations() {
        List<Participation> result = participationRepository.findByUser_Cid(user.getCid());

        assertEquals(2, result.size()); // event + otherEvent
    }

    @Test
    void findByUserCid_WhenUserHasOneParticipation() {
        List<Participation> result = participationRepository.findByUser_Cid(otherUser.getCid());

        assertEquals(1, result.size());
    }

    // ============ findByUser_CidAndEvent_Id (en kritik) ============

    @Test
    void findByUserAndEvent_WhenJoined_ShouldReturnPresent() {
        Optional<Participation> result = participationRepository
                .findByUser_CidAndEvent_Id(user.getCid(), event.getId());

        assertTrue(result.isPresent());
    }

    @Test
    void findByUserAndEvent_WhenNotJoined_ShouldReturnEmpty() {
        // otherUser, otherEvent'e katılmadı
        Optional<Participation> result = participationRepository
                .findByUser_CidAndEvent_Id(otherUser.getCid(), otherEvent.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void findByUserAndEvent_WhenUserDoesNotExist_ShouldReturnEmpty() {
        Optional<Participation> result = participationRepository
                .findByUser_CidAndEvent_Id(9999, event.getId());

        assertFalse(result.isPresent());
    }
}