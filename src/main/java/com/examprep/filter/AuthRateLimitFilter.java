package com.examprep.filter;

import com.examprep.config.AppConfig;
import com.examprep.i18n.Messages;
import com.examprep.util.RateLimiter;
import com.examprep.util.WebUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate-limits password and token forms so login stuffing cannot run unbounded.
 */
public class AuthRateLimitFilter implements Filter {

    private static final int SC_TOO_MANY_REQUESTS = 429;

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/login", "/register", "/forgot-password", "/reset-password"
    );

    private static final Map<String, String> ERROR_PAGES = Map.of(
            "/login", "/WEB-INF/jsp/auth/login.jsp",
            "/register", "/WEB-INF/jsp/auth/register.jsp",
            "/forgot-password", "/WEB-INF/jsp/auth/forgot-password.jsp",
            "/reset-password", "/WEB-INF/jsp/auth/reset-password.jsp"
    );

    private RateLimiter rateLimiter;
    private ScheduledExecutorService cleanupExecutor;

    @Override
    public void init(FilterConfig filterConfig) {
        int maxRequests = AppConfig.getInt("rate.limit.auth.max.requests", 8);
        long windowMinutes = AppConfig.getInt("rate.limit.auth.window.minutes", 15);
        rateLimiter = new RateLimiter(maxRequests, windowMinutes * 60 * 1000L);

        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auth-rate-limiter-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(rateLimiter::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (!"POST".equalsIgnoreCase(req.getMethod()) || !LIMITED_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = WebUtil.getClientIp(req);
        if (!rateLimiter.tryAcquire(clientIp)) {
            resp.setStatus(SC_TOO_MANY_REQUESTS);
            req.setAttribute("error", Messages.get(req, "error.rateLimited"));
            String page = ERROR_PAGES.getOrDefault(path, "/WEB-INF/jsp/auth/login.jsp");
            req.getRequestDispatcher(page).forward(req, resp);
            return;
        }

        chain.doFilter(request, response);
    }
}
