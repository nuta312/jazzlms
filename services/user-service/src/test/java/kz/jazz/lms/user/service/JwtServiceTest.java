package kz.jazz.lms.user.service;

import kz.jazz.lms.user.domain.User;
import kz.jazz.lms.user.domain.UserType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Пример unit-теста: без Spring-контекста, без БД — только чистая логика. */
class JwtServiceTest {

    private final JwtService jwt = new JwtService("test-secret-key-must-be-at-least-32-bytes-long!!", 60);

    @Test
    void generatedTokenContainsUserIdAndRole() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("john");
        u.setUserType(UserType.TRAINER);

        String token = jwt.generate(u);
        var claims = jwt.parse(token);

        assertEquals(u.getId().toString(), claims.getSubject());
        assertEquals("john", claims.get("username"));
        assertEquals("TRAINER", claims.get("role"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("john");
        String token = new JwtService("another-secret-key-that-is-also-32-bytes-long!!", 60).generate(u);

        assertThrows(Exception.class, () -> jwt.parse(token));
    }
}
