package com.examprep.filter;

import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.service.AuthService;
import com.examprep.util.JwtUtil;
import com.examprep.util.WebUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class JwtAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/login", "/register", "/css/", "/error/", "/api/access-tokens"
    );

    private final AuthService authService = new AuthService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        String token = WebUtil.getTokenFromCookie(req);
        User user = resolveUser(token);

        if (isPublicPath(path) || path.equals("/") || path.equals("/index.jsp")) {
            if (user != null) {
                req.setAttribute(WebUtil.CURRENT_USER_ATTR, user);
            }
            chain.doFilter(request, response);
            return;
        }

        if (user == null) {
            log.debug("Unauthenticated access to {}; redirecting to login", path);
            redirectToLogin(req, resp);
            return;
        }

        req.setAttribute(WebUtil.CURRENT_USER_ATTR, user);

        if (path.startsWith("/admin") && user.getRole() != Role.ADMIN) {
            log.warn("Forbidden admin access by user={} to {}", user.getUsername(), path);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        for (String publicPath : PUBLIC_PATHS) {
            if (publicPath.endsWith("/") && path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private User resolveUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = JwtUtil.parseToken(token);
            Long userId = JwtUtil.getUserId(claims);
            return authService.findById(userId).orElse(null);
        } catch (Exception e) {
            log.debug("Invalid or expired JWT: {}", e.getMessage());
            return null;
        }
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login");
    }
}
