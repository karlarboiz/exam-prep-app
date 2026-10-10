package com.examprep.servlet.user;

import com.examprep.model.User;
import com.examprep.service.AccessGrantService;
import com.examprep.util.DateFormats;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/subscription-expired")
public class SubscriptionExpiredServlet extends HttpServlet {

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
                    req.setAttribute("expiresAtLabel", DateFormats.format(grant.getExpiresAt()));
                }
            });
            req.getRequestDispatcher("/WEB-INF/jsp/user/subscription-expired.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
