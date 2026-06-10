package com.aiplatform.framework.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器(Sprint 5 Agent B)。
 * <p>
 * 行为契约:
 * <ol>
 *   <li>读取入站请求头 {@code X-Trace-Id},无值则生成 UUID</li>
 *   <li>放入 SLF4J MDC(key=traceId),日志格式可读出</li>
 *   <li>写入响应头 {@code X-Trace-Id} 供调用方关联</li>
 *   <li>try/finally 保证 MDC 清理,避免线程复用导致 traceId 串扰</li>
 * </ol>
 * 顺序:必须早于所有业务 Filter(JwtAuthenticationFilter / RateLimitFilter 等),
 * 设为 {@link Ordered#HIGHEST_PRECEDENCE}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
