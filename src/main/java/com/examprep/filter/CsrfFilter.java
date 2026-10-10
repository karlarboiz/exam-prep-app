package com.examprep.filter;

import com.examprep.util.CsrfUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF protection filter that validates tokens on state-changing requests.
 * Public API endpoints are exempt from CSRF validation (they use API key auth).
 */
public class CsrfFilter implements Filter {

    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/api/access-tokens",
            "/api/access-tokens/revoke"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isExempt(req, path)) {
            chain.doFilter(request, response);
            return;
        }

        if (!CsrfUtil.validateToken(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExempt(HttpServletRequest req, String path) {
        if (CsrfUtil.isExemptMethod(req)) {
            return true;
        }

        for (String exemptPath : EXEMPT_PATHS) {
            if (path.equals(exemptPath) || path.startsWith(exemptPath + "/")) {
                return true;
            }
        }

        return false;
    }
}
