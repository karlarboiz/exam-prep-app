package com.examprep.servlet.auth;

import com.examprep.i18n.LocaleSupport;
import com.examprep.model.AppLocale;
import com.examprep.model.User;
import com.examprep.service.AuthService;
import com.examprep.util.WebUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/locale")
public class LocaleServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppLocale locale = AppLocale.fromCode(req.getParameter("lang"));
        LocaleSupport.writeCookie(req, resp, locale);

        User user = WebUtil.getCurrentUser(req);
        if (user != null) {
            try {
                authService.updateLocale(user.getId(), locale);
            } catch (Exception ignored) {
                // Cookie still applies for this browser.
            }
        }

        resp.sendRedirect(req.getContextPath() + LocaleSupport.safeReturnPath(req));
    }
}
