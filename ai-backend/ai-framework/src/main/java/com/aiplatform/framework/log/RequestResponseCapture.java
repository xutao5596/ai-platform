package com.aiplatform.framework.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 审计日志辅助 Filter:将 Request/Response 包装为可重复读取的缓存版本,
 * 使下游 AOP 可在业务完成后读取完整 body。
 * <p>
 * 仅对 Controller 路径启用,避免对静态资源 / 大文件造成内存压力。
 */
@Component
public class RequestResponseCapture extends OncePerRequestFilter {

    private static final int MAX_CAPTURE_BYTES = 2 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return true;
        if (uri.startsWith("/actuator")) return true;
        if (uri.startsWith("/v3/api-docs")) return true;
        if (uri.startsWith("/swagger-ui")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean isFormOrJson = isCapturableBody(request);
        ContentCachingRequestWrapper reqWrap = isFormOrJson
                ? new ContentCachingRequestWrapper(request)
                : null;
        ContentCachingResponseWrapper respWrap = new ContentCachingResponseWrapper(response);

        try {
            if (reqWrap != null) {
                reqWrap.getInputStream();
            }
            filterChain.doFilter(reqWrap != null ? reqWrap : request, respWrap);
        } finally {
            try {
                respWrap.copyBodyToResponse();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isCapturableBody(HttpServletRequest req) {
        String ct = req.getContentType();
        if (ct == null) return false;
        String lower = ct.toLowerCase();
        if (lower.contains("application/json")
                || lower.contains("application/xml")
                || lower.contains("application/x-www-form-urlencoded")) {
            return true;
        }
        return lower.contains("text/")
                && req.getContentLengthLong() > 0
                && req.getContentLengthLong() <= MAX_CAPTURE_BYTES * 4L;
    }

    public static int maxCaptureBytes() {
        return MAX_CAPTURE_BYTES;
    }
}
