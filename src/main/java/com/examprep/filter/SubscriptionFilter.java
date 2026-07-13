package com.examprep.filter;

import com.examprep.model.Role;
import com.examprep.model.User;
import com.examprep.service.AccessGrantService;
import com.examprep.util.WebUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Ensures USER role requests under /user/** have an active (non-expired) access grant.
 * ADMIN users are not checked. /user/subscription-expired remains reachable.
 */
public class SubscriptionFilter implements Filter {

    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (!path.startsWith("/user")) {
            chain.doFilter(request, response);
            return;
        }

        if (path.equals("/user/subscription-expired") || path.startsWith("/user/subscription-expired/")) {
            chain.doFilter(request, response);
            return;
        }

        User user = WebUtil.getCurrentUser(req);
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        if (user.getRole() == Role.ADMIN) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (accessGrantService.hasActiveAccess(user.getId())) {
                chain.doFilter(request, response);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/user/subscription-expired");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
