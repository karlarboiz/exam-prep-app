package com.examprep.filter;

import com.examprep.config.AppConfig;
import com.examprep.util.RateLimiter;
import com.examprep.util.SimpleJson;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter for API endpoints.
 * Prevents brute force attacks and DoS by limiting requests per IP address.
 */
public class ApiRateLimitFilter implements Filter {

    private final static int SC_TOO_MANY_REQUESTS = 429;

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/access-tokens",
            "/api/access-tokens/revoke"
    );

    private RateLimiter rateLimiter;
    private ScheduledExecutorService cleanupExecutor;

    @Override
    public void init(FilterConfig filterConfig) {
        int maxRequests = AppConfig.getInt("rate.limit.api.max.requests", 10);
        long windowMinutes = AppConfig.getInt("rate.limit.api.window.minutes", 1);
        rateLimiter = new RateLimiter(maxRequests, windowMinutes * 60 * 1000);

        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(
                rateLimiter::cleanup,
                5,
                5,
                TimeUnit.MINUTES
        );
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

        if (!isRateLimited(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(req);
        if (!rateLimiter.tryAcquire(clientIp)) {
            int remaining = rateLimiter.getRemainingRequests(clientIp);
            resp.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            resp.setStatus(SC_TOO_MANY_REQUESTS);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(SimpleJson.object("error", "Rate limit exceeded. Please try again later."));
            return;
        }

        int remaining = rateLimiter.getRemainingRequests(clientIp);
        resp.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String path) {
        for (String limitedPath : RATE_LIMITED_PATHS) {
            if (path.equals(limitedPath) || path.startsWith(limitedPath + "/")) {
                return true;
            }
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) {
                ip = ip.substring(0, commaIndex).trim();
            }
        } else {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
