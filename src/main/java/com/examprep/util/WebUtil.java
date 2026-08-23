package com.examprep.util;

import com.examprep.config.AppConfig;
import com.examprep.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class WebUtil {

    private WebUtil() {
    }

    public static final String CURRENT_USER_ATTR = "currentUser";

    public static void setAuthCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        addAuthCookie(request, response, token, 60 * 60 * 24);
    }

    public static void clearAuthCookie(HttpServletRequest request, HttpServletResponse response) {
        addAuthCookie(request, response, "", 0);
    }

    /**
     * Honor {@code cookie.secure} / {@code cookie.samesite}. Behind a TLS terminator,
     * set {@code proxy.trust.forwarded=true} so {@code X-Forwarded-Proto} is trusted.
     */
    private static void addAuthCookie(HttpServletRequest request, HttpServletResponse response,
                                      String token, int maxAge) {
        String sameSite = normalizeSameSite(AppConfig.get("cookie.samesite", "Lax"));
        boolean secure = AppConfig.getBoolean("cookie.secure", false)
                || isHttps(request)
                || "None".equals(sameSite);

        Cookie cookie = new Cookie(JwtUtil.COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);
    }

    public static boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        if (!AppConfig.getBoolean("proxy.trust.forwarded", false)) {
            return false;
        }
        String forwarded = request.getHeader("X-Forwarded-Proto");
        if (forwarded == null || forwarded.isBlank()) {
            return false;
        }
        String first = forwarded.split(",")[0].trim();
        return "https".equalsIgnoreCase(first);
    }

    public static String getClientIp(HttpServletRequest request) {
        if (AppConfig.getBoolean("proxy.trust.forwarded", false)) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                int commaIndex = ip.indexOf(',');
                return commaIndex > 0 ? ip.substring(0, commaIndex).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static String normalizeSameSite(String value) {
        if (value == null || value.isBlank()) {
            return "Lax";
        }
        String trimmed = value.trim();
        if ("Strict".equalsIgnoreCase(trimmed)) {
            return "Strict";
        }
        if ("None".equalsIgnoreCase(trimmed)) {
            return "None";
        }
        return "Lax";
    }

    public static String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (JwtUtil.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static User getCurrentUser(HttpServletRequest request) {
        Object user = request.getAttribute(CURRENT_USER_ATTR);
        if (user instanceof User u) {
            return u;
        }
        return null;
    }

    public static String contextPath(HttpServletRequest request) {
        return request.getContextPath();
    }
}
