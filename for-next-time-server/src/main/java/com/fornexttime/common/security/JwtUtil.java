package com.fornexttime.common.security;

import com.fornexttime.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final JwtProperties properties;

    /**
     * 注入 JWT 配置，用于统一读取密钥和有效期。
     */
    public JwtUtil(JwtProperties properties) {
        // 1. 保存不可变配置供签发和解析复用。
        this.properties = properties;
    }

    /**
     * 为已认证用户生成带有效期的 JWT。
     */
    public String createToken(Long userId, String username) {
        // 1. 计算签发时间与到期时间。
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.expirationSeconds());
        // 2. 写入用户身份声明并使用配置密钥签名。
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    /**
     * 校验 JWT 签名和过期时间，并提取当前用户身份。
     */
    public JwtUser parseToken(String token) {
        // 1. 解析签名令牌，JWT 库会在异常时终止认证。
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // 2. 将声明转换为应用内的用户上下文对象。
        try {
            return new JwtUser(Long.valueOf(claims.getSubject()), claims.get("username", String.class));
        } catch (NumberFormatException exception) {
            throw new SecurityException("Token subject is invalid", exception);
        }
    }

    /**
     * 将配置中的 JWT 密钥转换为符合 HS256 长度要求的签名密钥。
     */
    private SecretKey signingKey() {
        // 1. 拒绝缺失或长度不足的开发、生产配置。
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        // 2. 使用 UTF-8 字节创建签名密钥。
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
