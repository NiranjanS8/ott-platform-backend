package com.ott.streaming.security;

import com.ott.streaming.config.properties.JwtProperties;
import com.ott.streaming.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(hashSecret(jwtProperties.secret()));
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getEmail())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .claims(Map.of("role", user.getRole().name(), "name", user.getName()))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        boolean subjectMatches = claims.getSubject().equals(userDetails.getUsername());
        boolean notExpired = claims.getExpiration().after(new Date());
        return subjectMatches && notExpired;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private byte[] hashSecret(String secret) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to initialize JWT signing key", ex);
        }
    }
}
