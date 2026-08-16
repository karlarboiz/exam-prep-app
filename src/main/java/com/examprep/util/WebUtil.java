package com.examprep.util;

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
     * Ngrok and other HTTPS proxies terminate TLS before Tomcat, so {@code request.isSecure()}
     * is often false. Honor {@code X-Forwarded-Proto} and mark the cookie Secure + SameSite=Lax
     * so browsers keep the session on the public https:// host.
     */
    private static void addAuthCookie(HttpServletRequest request, HttpServletResponse response,
                                      String token, int maxAge) {
        Cookie cookie = new Cookie(JwtUtil.COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setSecure(isHttps(request));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    static boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwarded = request.getHeader("X-Forwarded-Proto");
        if (forwarded == null || forwarded.isBlank()) {
            return false;
        }
        String first = forwarded.split(",")[0].trim();
        return "https".equalsIgnoreCase(first);
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
