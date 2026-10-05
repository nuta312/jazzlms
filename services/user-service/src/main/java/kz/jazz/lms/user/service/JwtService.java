package kz.jazz.lms.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kz.jazz.lms.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT = header.payload.signature (base64). Подписан секретом сервера,
 * поэтому клиент не может подделать payload (userId, role).
 * Тот же секрет знает api-gateway и проверяет подпись на каждом запросе.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(User user) { return generate(user, null); }

    /** impersonatedBy != null — админ вошёл "под пользователем"; id админа остаётся в токене для аудита. */
    public String generate(User user, UUID impersonatedBy) {
        Instant now = Instant.now();
        var builder = Jwts.builder();
        if (impersonatedBy != null) builder.claim("impersonatedBy", impersonatedBy.toString());
        return builder
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getUserType().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
