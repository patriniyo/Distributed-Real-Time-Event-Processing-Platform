package com.drep.ingestion.auth;

import com.drep.ingestion.config.IngestionProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtService {

    private final IngestionProperties properties;
    private final SecretKey secretKey;

    public JwtService(IngestionProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(
                properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Optional<String> validateAndExtractTenant(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration().before(new Date())) {
                return Optional.empty();
            }

            String tenantId = claims.get("tenantId", String.class);
            if (tenantId == null || tenantId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(tenantId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public String generateToken(String tenantId, long expirationMs) {
        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(tenantId)
                .claim("tenantId", tenantId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }
}
