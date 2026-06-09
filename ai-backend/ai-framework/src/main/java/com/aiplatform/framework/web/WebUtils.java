package com.aiplatform.framework.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Web / 网络工具。
 */
@UtilityClass
public class WebUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCAL_IPV4 = "127.0.0.1";
    private static final String LOCAL_IPV6 = "0:0:0:0:0:0:0:1";

    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (isUnknown(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isUnknown(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isUnknown(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isUnknown(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isUnknown(ip)) {
            ip = request.getRemoteAddr();
        }
        if (LOCAL_IPV6.equals(ip)) {
            ip = LOCAL_IPV4;
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? UNKNOWN : ip;
    }

    public String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return LOCAL_IPV4;
        }
    }

    private boolean isUnknown(String ip) {
        return ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip);
    }
}
