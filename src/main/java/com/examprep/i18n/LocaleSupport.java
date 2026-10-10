package com.examprep.i18n;

import com.examprep.model.AppLocale;
import com.examprep.util.WebUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URI;

public final class LocaleSupport {

    public static final String COOKIE_NAME = "locale";
    public static final String REQUEST_ATTR = "appLocale";
    public static final String CURRENT_PATH_ATTR = "currentPath";
    public static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

    private static final ThreadLocal<AppLocale> CURRENT = new ThreadLocal<>();

    private LocaleSupport() {
    }

    public static void setCurrent(AppLocale locale) {
        CURRENT.set(locale != null ? locale : AppLocale.DEFAULT);
    }

    public static AppLocale current() {
        AppLocale locale = CURRENT.get();
        return locale != null ? locale : AppLocale.DEFAULT;
    }

    public static AppLocale current(HttpServletRequest request) {
        Object attr = request.getAttribute(REQUEST_ATTR);
        if (attr instanceof AppLocale locale) {
            return locale;
        }
        return current();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static AppLocale fromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && AppLocale.isSupported(cookie.getValue())) {
                return AppLocale.fromCode(cookie.getValue());
            }
        }
        return null;
    }

    public static void writeCookie(HttpServletRequest request, HttpServletResponse response, AppLocale locale) {
        Cookie cookie = new Cookie(COOKIE_NAME, locale.getCode());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setSecure(WebUtil.isHttps(request));
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public static String currentRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            path = path + "?" + query;
        }
        return path;
    }

    public static String safeReturnPath(HttpServletRequest request) {
        String returnTo = request.getParameter("returnTo");
        if (isSafeRelativePath(returnTo) && !returnTo.startsWith("/locale")) {
            return returnTo;
        }
        String fromReferer = pathFromReferer(request);
        if (fromReferer != null) {
            return fromReferer;
        }
        return "/";
    }

    static boolean isSafeRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String target = path.split("\\?", 2)[0];
        return target.startsWith("/")
                && !target.startsWith("//")
                && !path.contains("\\")
                && !path.contains("://");
    }

    private static String pathFromReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            String context = request.getContextPath();
            if (!context.isEmpty() && path.startsWith(context)) {
                path = path.substring(context.length());
            }
            if (path.isEmpty()) {
                path = "/";
            }
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                path = path + "?" + uri.getQuery();
            }
            if (isSafeRelativePath(path) && !path.startsWith("/locale")) {
                return path;
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }
}
