package com.aiplatform.project.security;

import com.aiplatform.common.exception.BusinessException;
import com.aiplatform.common.exception.ErrorCode;
import com.aiplatform.project.annotation.PreProjectRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PreProjectRoleAspect {

    private final ProjectRoleChecker checker;

    @Around("@annotation(ppr)")
    public Object checkAnnotation(ProceedingJoinPoint pjp, PreProjectRole ppr) throws Throwable {
        return doCheck(pjp, ppr);
    }

    @Around("@within(ppr)")
    public Object checkWithin(ProceedingJoinPoint pjp, PreProjectRole ppr) throws Throwable {
        return doCheck(pjp, ppr);
    }

    private Object doCheck(ProceedingJoinPoint pjp, PreProjectRole ppr) throws Throwable {
        if (ppr == null) {
            // @within 形式下若类上无注解,ppr 可能为 null,直接放行(由调用方 @annotation 保证)
            return pjp.proceed();
        }
        Long projectId = resolveProjectId(pjp);
        log.debug("PreProjectRole: projectId={}, required={}, uri={}", projectId, ppr.value(), currentUri());
        checker.requireAtLeast(projectId, ppr.value());
        return pjp.proceed();
    }

    private String currentUri() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? "?" : attrs.getRequest().getRequestURI();
        } catch (Exception e) { return "?"; }
    }

    private Long resolveProjectId(ProceedingJoinPoint pjp) {
        // 1. 优先从 URI 模板变量取值(@PathVariable 编译擦除不影响 URI 模板)
        Long fromUri = extractFromUri("projectId");
        if (fromUri != null) return fromUri;

        // 2. URI 中 {id} 可能是 entity id (flow/assistant 等),需要按 entity 类型反查 projectId
        Long idFromUri = extractFromUri("id");
        if (idFromUri != null) {
            String uri = currentUri();
            Long resolved = resolveEntityProjectId(uri, idFromUri);
            if (resolved != null) return resolved;
        }

        // 3. 回退:方法参数上的 @PathVariable 显式 value
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Annotation[][] pas = method.getParameterAnnotations();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < args.length; i++) {
            for (Annotation a : pas[i]) {
                if (a instanceof PathVariable pv) {
                    String name = pv.value();
                    if ("projectId".equals(name) || "id".equals(name)) {
                        return toLong(args[i]);
                    }
                }
            }
        }
        // 4. 回退:参数对象字段(直接 projectId 字段)
        if (args.length >= 1 && args[0] != null) {
            for (String fieldName : new String[]{"projectId", "id"}) {
                try {
                    var f = args[0].getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object v = f.get(args[0]);
                    Long lv = toLong(v);
                    if (lv != null) return lv;
                } catch (NoSuchFieldException ignored) {
                } catch (Exception e) {
                    log.debug("Failed to read {} field: {}", fieldName, e.getMessage());
                }
            }
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析项目 id");
    }

    /**
     * 根据 URI 模式将 {id} 解析为对应的 entity.projectId
     * 支持: /api/v1/flow/{id}/*  → ai_flow.project_id
     *       /api/v1/assistant/{id}/* → ai_assistant.project_id
     *       /api/v1/trigger/{id}/* → ai_flow_trigger.project_id
     *       /api/v1/version/{id}/* → ai_flow_version.project_id (经 ai_flow)
     */
    private Long resolveEntityProjectId(String uri, Long id) {
        try {
            if (uri == null) return null;
            if (uri.startsWith("/api/v1/flow/") || uri.startsWith("/api/v1/flow")) {
                return lookupProjectId("ai_flow", "id", id);
            }
            if (uri.startsWith("/api/v1/assistant/") || uri.startsWith("/api/v1/assistant")) {
                return lookupProjectId("ai_assistant", "id", id);
            }
            if (uri.startsWith("/api/v1/trigger/") || uri.startsWith("/api/v1/trigger")) {
                return lookupProjectId("ai_flow_trigger", "id", id);
            }
        } catch (Exception e) {
            log.debug("resolveEntityProjectId failed for uri={} id={}: {}", uri, id, e.getMessage());
        }
        return null;
    }

    private Long lookupProjectId(String table, String idCol, Long id) {
        try {
            org.springframework.jdbc.core.JdbcTemplate jdbc = applicationContext.getBean(
                    org.springframework.jdbc.core.JdbcTemplate.class);
            String sql = "SELECT project_id FROM " + table + " WHERE " + idCol + " = ? AND deleted = 0";
            try {
                return jdbc.queryForObject(sql, Long.class, id);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                return null;
            }
        } catch (Exception e) {
            log.debug("lookupProjectId({}) failed: {}", table, e.getMessage());
            return null;
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    private Long extractFromUri(String varName) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            Map<String, String> uriTemplateVariables =
                    (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriTemplateVariables == null) return null;
            String v = uriTemplateVariables.get(varName);
            return v == null ? null : toLong(v);
        } catch (Exception e) {
            log.debug("Failed to extract {} from URI: {}", varName, e.getMessage());
            return null;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
