package com.aiplatform.project.security;

import com.aiplatform.common.util.JsonUtils;
import com.aiplatform.framework.observability.BusinessMetrics;
import com.aiplatform.framework.security.ApiKeyContext;
import com.aiplatform.project.entity.AiProjectApiKey;
import com.aiplatform.project.mapper.AiProjectApiKeyMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * API Key 鉴权过滤器:拦截 /api/v1/ext/**。
 * Header 格式:
 * 1. Authorization: ApiKey <apiKey>:<apiSecret>
 * 2. X-API-Key + X-API-Secret(等价)
 * 校验通过后设置 ApiKeyContext(项目 ID + scope 列表 + 限额)。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PROTECTED_PATTERNS = List.of("/api/v1/ext/**");

    private static final String PREFIX = "ApiKey ";

    private final AiProjectApiKeyMapper apiKeyMapper;

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
        String[] creds = resolveCredentials(request);
        if (creds == null) {
            BusinessMetrics.apiKeyCall(null, request.getRequestURI(), "unauthorized");
            writeUnauthorized(response, "缺少 API Key 凭证");
            return;
        }
        String apiKey = creds[0];
        String apiSecret = creds[1];
        AiProjectApiKey record = apiKeyMapper.selectByApiKey(apiKey);
        if (record == null) {
            BusinessMetrics.apiKeyCall(null, request.getRequestURI(), "not_found");
            writeUnauthorized(response, "API Key 不存在");
            return;
        }
        if (record.getStatus() == null || record.getStatus() != 1) {
            BusinessMetrics.apiKeyCall(record.getId(), request.getRequestURI(), "disabled");
            writeUnauthorized(response, "API Key 已禁用");
            return;
        }
        if (record.getApiSecret() == null || !record.getApiSecret().equals(apiSecret)) {
            BusinessMetrics.apiKeyCall(record.getId(), request.getRequestURI(), "secret_mismatch");
            writeUnauthorized(response, "API Secret 不匹配");
            return;
        }
        if (record.getExpiresAt() != null && record.getExpiresAt() > 0
                && record.getExpiresAt() < System.currentTimeMillis()) {
            BusinessMetrics.apiKeyCall(record.getId(), request.getRequestURI(), "expired");
            writeUnauthorized(response, "API Key 已过期");
            return;
        }

        List<String> scopes = parseScopes(record.getScopes());
        ApiKeyContext ctx = new ApiKeyContext(record.getId(), record.getProjectId(), scopes);
        ApiKeyContext.set(ctx);
        try {
            // 写入限流过滤器需要的属性
            request.setAttribute("X-ApiKey-Limit", record.getRateLimit() == null ? 60 : record.getRateLimit());
            // 异步更新最后使用时间/IP(此处同步写一次,后续可改为异步)
            try {
                record.setLastUsedTime(LocalDateTime.now());
                String ip = resolveClientIp(request);
                record.setLastUsedIp(ip);
                apiKeyMapper.updateById(record);
            } catch (Exception logEx) {
                log.debug("Update apiKey lastUsed info failed: {}", logEx.getMessage());
            }
            chain.doFilter(request, response);
            BusinessMetrics.apiKeyCall(record.getId(), request.getRequestURI(), "success");
        } catch (Throwable t) {
            BusinessMetrics.apiKeyCall(record.getId(), request.getRequestURI(), "error");
            throw t;
        } finally {
            ApiKeyContext.clear();
        }
    }

    private String[] resolveCredentials(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith(PREFIX)) {
            String body = auth.substring(PREFIX.length()).trim();
            int idx = body.indexOf(':');
            if (idx > 0) {
                return new String[]{body.substring(0, idx), body.substring(idx + 1)};
            }
        }
        String key = request.getHeader("X-API-Key");
        String secret = request.getHeader("X-API-Secret");
        if (key != null && secret != null && !key.isBlank() && !secret.isBlank()) {
            return new String[]{key.trim(), secret.trim()};
        }
        return null;
    }

    private List<String> parseScopes(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<String> list = JsonUtils.fromJson(json, new TypeReference<List<String>>() {
            });
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int idx = ip.indexOf(',');
            return (idx > 0 ? ip.substring(0, idx) : ip).trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        return request.getRemoteAddr();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = JsonUtils.toJson(Map.of("code", 401, "message", message));
        response.getWriter().write(body);
    }
}
