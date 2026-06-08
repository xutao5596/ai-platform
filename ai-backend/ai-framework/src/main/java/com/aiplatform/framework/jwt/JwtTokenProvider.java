package com.aiplatform.framework.jwt;

import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JWT 令牌签发与解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;
    private SecretKey secretKey;

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
            secretKey = Keys.hmacShaKeyFor(padTo32Bytes(bytes));
        }
        return secretKey;
    }

    private byte[] padTo32Bytes(byte[] src) {
        if (src.length >= 32) {
            return src;
        }
        byte[] dst = new byte[32];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    public String generateAccessToken(LoginUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getAccessTtlSeconds() * 1000L);
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(now)
                .expiration(exp)
                .claims(buildClaims(user))
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(LoginUser user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getRefreshTtlSeconds() * 1000L);
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(now)
                .expiration(exp)
                .id("refresh")
                .signWith(getSecretKey())
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }

    public LoginUser toLoginUser(Claims claims) {
        if (claims == null) {
            return null;
        }
        LoginUser.LoginUserBuilder b = LoginUser.builder()
                .userId(Long.valueOf(claims.getSubject()))
                .username((String) claims.get(SecurityConstants.JWT_CLAIMS_USERNAME));

        Object roles = claims.get(SecurityConstants.JWT_CLAIMS_ROLES);
        if (roles instanceof java.util.Collection<?> c) {
            Set<String> set = new HashSet<>();
            for (Object o : c) {
                if (o != null) set.add(String.valueOf(o));
            }
            b.roles(set);
        }
        Object perms = claims.get(SecurityConstants.JWT_CLAIMS_PERMS);
        if (perms instanceof java.util.Collection<?> c) {
            Set<String> set = new HashSet<>();
            for (Object o : c) {
                if (o != null) set.add(String.valueOf(o));
            }
            b.permissions(set);
        }
        return b.build();
    }

    private Map<String, Object> buildClaims(LoginUser user) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put(SecurityConstants.JWT_CLAIMS_USER_ID, user.getUserId());
        claims.put(SecurityConstants.JWT_CLAIMS_USERNAME, user.getUsername());
        if (user.getRoles() != null) {
            claims.put(SecurityConstants.JWT_CLAIMS_ROLES, user.getRoles());
        }
        if (user.getPermissions() != null) {
            claims.put(SecurityConstants.JWT_CLAIMS_PERMS, user.getPermissions());
        }
        return claims;
    }

    public long getAccessTtlSeconds() {
        return props.getAccessTtlSeconds();
    }
}
