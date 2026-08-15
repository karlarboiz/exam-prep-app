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

    public static void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(JwtUtil.COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);
        
        boolean secure = AppConfig.getBoolean("cookie.secure", false);
        cookie.setSecure(secure);
        
        String sameSite = AppConfig.get("cookie.samesite", "Lax");
        response.setHeader("Set-Cookie", buildSetCookieHeader(cookie, sameSite));
    }

    public static void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JwtUtil.COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        
        boolean secure = AppConfig.getBoolean("cookie.secure", false);
        cookie.setSecure(secure);
        
        String sameSite = AppConfig.get("cookie.samesite", "Lax");
        response.setHeader("Set-Cookie", buildSetCookieHeader(cookie, sameSite));
    }

    private static String buildSetCookieHeader(Cookie cookie, String sameSite) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
        sb.append("; Path=").append(cookie.getPath());
        
        if (cookie.getMaxAge() >= 0) {
            sb.append("; Max-Age=").append(cookie.getMaxAge());
        }
        
        if (cookie.isHttpOnly()) {
            sb.append("; HttpOnly");
        }
        
        if (cookie.getSecure()) {
            sb.append("; Secure");
        }
        
        if (sameSite != null && !sameSite.isBlank()) {
            sb.append("; SameSite=").append(sameSite);
        }
        
        return sb.toString();
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
