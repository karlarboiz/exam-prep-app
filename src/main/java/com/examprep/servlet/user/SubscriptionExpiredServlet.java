package com.examprep.servlet.user;

import com.examprep.model.User;
import com.examprep.service.AccessGrantService;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@WebServlet("/user/subscription-expired")
public class SubscriptionExpiredServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiredServlet.class);

    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        try {
            accessGrantService.findLatestRedeemed(user.getId()).ifPresent(grant -> {
                req.setAttribute("expiredGrant", grant);
                if (grant.getExpiresAt() != null) {
                    req.setAttribute("expiresAtLabel",
                            grant.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
            });
            req.getRequestDispatcher("/WEB-INF/jsp/user/subscription-expired.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("Failed to render subscription-expired page for user={}", user.getUsername(), e);
            throw new ServletException(e);
        }
    }
}
