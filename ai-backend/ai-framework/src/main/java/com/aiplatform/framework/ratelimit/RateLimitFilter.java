package com.aiplatform.framework.ratelimit;

import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.framework.exception.RateLimitException;
import com.aiplatform.framework.security.ApiKeyContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 限流过滤器:在 ApiKeyAuthFilter 之后执行,从 ApiKeyContext 取 keyId + 限额做计数。
 * 仅作用于 /api/v1/ext/**。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PROTECTED_PATTERNS = List.of("/api/v1/ext/**");

    private final RateLimiter rateLimiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String p : PROTECTED_PATTERNS) {
            if (MATCHER.match(p, path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ApiKeyContext ctx = ApiKeyContext.get();
        if (ctx == null) {
            // 没有 ApiKey 上下文(应该是匿名或内部调用),放行(由 ApiKeyAuthFilter 控制)
            chain.doFilter(request, response);
            return;
        }
        Integer limit = null;
        try {
            // 从请求 attribute 中取 key 限额(由 ApiKeyAuthFilter 写入)
            Object l = request.getAttribute("X-ApiKey-Limit");
            if (l instanceof Integer) {
                limit = (Integer) l;
            }
        } catch (Exception ignored) {
        }
        int effective = limit == null ? rateLimiter.getDefaultLimit() : limit;
        try {
            rateLimiter.check(ctx.getApiKeyIdRaw(), effective);
        } catch (RateLimitException ex) {
            log.warn("Rate limit hit: apiKeyId={}, limit={}", ctx.getApiKeyIdRaw(), effective);
            writeRateLimited(response, ex);
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeRateLimited(HttpServletResponse response, RateLimitException ex) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        String body = JsonUtils.toJson(Map.of(
                "code", ex.getCode(),
                "message", ex.getMessage()
        ));
        response.getWriter().write(body);
    }
}
