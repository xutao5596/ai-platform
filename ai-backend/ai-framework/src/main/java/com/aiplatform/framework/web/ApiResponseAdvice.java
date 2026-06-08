package com.aiplatform.framework.web;

import com.aiplatform.common.context.UserContext;
import com.aiplatform.common.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一响应包装(可选,保持 Result 直接返回)。
 * 当前默认不包装,只做日志追踪。
 */
@RestControllerAdvice(basePackages = "com.aiplatform")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired(required = false)
    private JsonUtils json;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return false;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (request instanceof ServletServerHttpRequest req) {
            HttpServletRequest http = req.getServletRequest();
            long uid = UserContext.getUserId() == null ? 0L : UserContext.getUserId();
            if (body instanceof com.aiplatform.common.api.Result<?> r) {
                if (r.getTimestamp() == 0) {
                    r.setTimestamp(System.currentTimeMillis());
                }
            }
            if (http.getAttribute("__request_id__") == null) {
                http.setAttribute("__request_id__", java.util.UUID.randomUUID().toString());
            }
        }
        return body;
    }
}
