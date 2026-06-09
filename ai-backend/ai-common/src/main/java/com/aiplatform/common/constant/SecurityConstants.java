package com.aiplatform.common.constant;

/**
 * 安全相关常量(JWT / Shiro / 鉴权头)。
 */
public interface SecurityConstants {

    String AUTHORIZATION_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";

    String JWT_CLAIMS_USER_ID = "uid";
    String JWT_CLAIMS_USERNAME = "uname";
    String JWT_CLAIMS_ROLES = "roles";
    String JWT_CLAIMS_PERMS = "perms";

    String LOGIN_USER_KEY = "aiplatform:loginUser:";
    String TOKEN_BLACKLIST_KEY = "aiplatform:token:blacklist:";

    long DEFAULT_TOKEN_TTL_SECONDS = 7 * 24 * 3600L;
    long DEFAULT_REFRESH_TOKEN_TTL_SECONDS = 30 * 24 * 3600L;

    String LOGIN_URL = "/api/v1/auth/login";
    String LOGOUT_URL = "/api/v1/auth/logout";
    String REFRESH_URL = "/api/v1/auth/refresh";

    String ANONYMOUS_USER = "anonymous";
}
