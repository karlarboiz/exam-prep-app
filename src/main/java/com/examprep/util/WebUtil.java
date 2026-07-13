package com.examprep.util;

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
        response.addCookie(cookie);
    }

    public static void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JwtUtil.COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
