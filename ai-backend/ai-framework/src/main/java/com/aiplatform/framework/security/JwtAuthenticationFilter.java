package com.aiplatform.framework.security;

import com.aiplatform.common.context.LoginUser;
import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.framework.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 鉴权过滤器(基于 Spring,不依赖 Shiro 过滤器链)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> SKIP_PATTERNS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/captcha",
            "/api/v1/webhook/**",
            "/api/v1/public/**",
            "/druid/**",
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/error",
            "/favicon.ico",
            "/static/**"
    );

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : SKIP_PATTERNS) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            writeUnauthorized(response, "未登录或登录已过期");
            return;
        }
        Claims claims = jwtTokenProvider.parse(token);
        if (claims == null) {
            writeUnauthorized(response, "Token 无效或已过期");
            return;
        }
        LoginUser user = jwtTokenProvider.toLoginUser(claims);
        if (user == null) {
            writeUnauthorized(response, "Token 内容无效");
            return;
        }
        user.setToken(token);
        UserContext.set(user);
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7).trim();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = JsonUtils.toJson(java.util.Map.of("code", 401, "message", message));
        response.getWriter().write(body);
    }
}
