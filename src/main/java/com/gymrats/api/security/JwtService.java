package com.gymrats.api.security;

import com.gymrats.api.user.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expirationSeconds;
  public JwtService(@Value("${app.security.jwt.secret}") String secret, @Value("${app.security.jwt.expiration-seconds}") long expirationSeconds) { this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); this.expirationSeconds = expirationSeconds; }
  public String generate(User user) { var now = Instant.now(); return Jwts.builder().subject(user.getEmail()).claim("role", user.getRole().name()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact(); }
  public String subject(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject(); }
  public long expirationSeconds() { return expirationSeconds; }
}
