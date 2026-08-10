package com.examprep.filter;

import com.examprep.dao.UserDao;
import com.examprep.model.Role;
import com.examprep.model.User;
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
 * Hard-gates USER role requests under /user/** until the placement diagnostic is completed.
 * Allows /user/diagnostic and /user/diagnostic/result. ADMIN users are not checked.
 */
public class DiagnosticFilter implements Filter {

    private final UserDao userDao = new UserDao();

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

        if (path.equals("/user/diagnostic")
                || path.startsWith("/user/diagnostic/")
                || path.equals("/user/subscription-expired")
                || path.startsWith("/user/subscription-expired/")) {
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
            if (userDao.isDiagnosticCompleted(user.getId())) {
                chain.doFilter(request, response);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/user/diagnostic");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
