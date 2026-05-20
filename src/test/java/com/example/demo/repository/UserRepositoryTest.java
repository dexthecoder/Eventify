package com.example.demo.repository;

import com.example.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    private User createUser(String email, String phone, boolean enabled) {
        User u = new User();
        u.setName("Test");
        u.setSurname("User");
        u.setEmail(email);
        u.setPhone(phone);
        u.setPassword("hashedpw");
        u.setEnabled(enabled);
        return u;
    }

    @BeforeEach
    void setUp() {
        em.persist(createUser("polat@test.com", "05551234567", true));
        em.persist(createUser("disabled@test.com", "05559999999", false));
        em.flush();
    }

    @Test
    void findByEmailOrPhone_WhenEmailMatches_ShouldReturnUser() {
        List<User> result = userRepository
                .findByEmailEqualsOrPhoneEqualsAllIgnoreCase("polat@test.com", "00000000000");

        assertEquals(1, result.size());
        assertEquals("polat@test.com", result.get(0).getEmail());
    }

    @Test
    void findByEmailOrPhone_WhenPhoneMatches_ShouldReturnUser() {
        List<User> result = userRepository
                .findByEmailEqualsOrPhoneEqualsAllIgnoreCase("yok@test.com", "05551234567");

        assertEquals(1, result.size());
    }

    @Test
    void findByEmailOrPhone_CaseInsensitive() {
        List<User> result = userRepository
                .findByEmailEqualsOrPhoneEqualsAllIgnoreCase("POLAT@TEST.COM", "00000000000");

        assertEquals(1, result.size());
    }

    @Test
    void findByEmailOrPhone_WhenNoMatch_ShouldReturnEmpty() {
        List<User> result = userRepository
                .findByEmailEqualsOrPhoneEqualsAllIgnoreCase("yok@test.com", "00000000000");

        assertTrue(result.isEmpty());
    }

    @Test
    void findForLogin_WhenEnabledAndEmailMatches_ShouldReturnUser() {
        Optional<User> result = userRepository
                .findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(
                        "polat@test.com", "polat@test.com");

        assertTrue(result.isPresent());
    }

    @Test
    void findForLogin_WhenDisabled_ShouldReturnEmpty() {
        Optional<User> result = userRepository
                .findByEnabledTrueAndEmailIgnoreCaseOrEnabledTrueAndPhoneIgnoreCase(
                        "disabled@test.com", "disabled@test.com");

        assertFalse(result.isPresent());
    }
}