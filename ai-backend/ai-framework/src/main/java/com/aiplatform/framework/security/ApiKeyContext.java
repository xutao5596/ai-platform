package com.aiplatform.framework.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * API Key 鉴权上下文(基于 ThreadLocal)。
 * 在 ApiKeyAuthFilter 通过后设置,业务代码可静态访问 projectId / scopes / apiKeyId。
 */
public class ApiKeyContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final ThreadLocal<ApiKeyContext> CONTEXT = new ThreadLocal<>();

    private Long apiKeyId;
    private Long projectId;
    private List<String> scopes;

    public ApiKeyContext() {
    }

    public ApiKeyContext(Long apiKeyId, Long projectId, List<String> scopes) {
        this.apiKeyId = apiKeyId;
        this.projectId = projectId;
        this.scopes = scopes == null ? Collections.emptyList() : scopes;
    }

    public static void set(ApiKeyContext ctx) {
        CONTEXT.set(ctx);
    }

    public static ApiKeyContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long getProjectId() {
        ApiKeyContext c = CONTEXT.get();
        return c == null ? null : c.projectId;
    }

    public static Long getApiKeyId() {
        ApiKeyContext c = CONTEXT.get();
        return c == null ? null : c.apiKeyId;
    }

    public static List<String> getScopes() {
        ApiKeyContext c = CONTEXT.get();
        return c == null ? Collections.emptyList() : c.scopes;
    }

    public static boolean hasScope(String scope) {
        if (scope == null) return false;
        List<String> s = getScopes();
        if (s == null) return false;
        if (s.contains("*")) return true;
        return s.contains(scope);
    }

    public Long getApiKeyIdRaw() {
        return apiKeyId;
    }

    public Long getProjectIdRaw() {
        return projectId;
    }

    public List<String> getScopesRaw() {
        return scopes;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? Collections.emptyList() : scopes;
    }
}
