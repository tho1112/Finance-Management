package com.example.demo.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.security.Keys;
import java.security.Key;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret:DayLaChuoiBiMatSieuCapVipProDungDeKyTokenChoDuAnNhanhNhat2026CuaToi}")
    private String jwtSecret;

    private final long JWT_EXPIRATION = 604800000L;

    // Hàm bổ trợ để tạo Key chuẩn từ chuỗi String trên
    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + JWT_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("[JwtTokenProvider] Chữ ký JWT không hợp lệ!");
        } catch (ExpiredJwtException e) {
            log.warn("[JwtTokenProvider] JWT Token đã hết hạn!");
        } catch (UnsupportedJwtException e) {
            log.warn("[JwtTokenProvider] JWT Token không được hỗ trợ!");
        } catch (IllegalArgumentException e) {
            log.warn("[JwtTokenProvider] JWT claims string bị rỗng!");
        } catch (Exception ex) {
            log.error("[JwtTokenProvider] Lỗi xác thực JWT: {}", ex.getMessage());
        }
        return false;
    }
}