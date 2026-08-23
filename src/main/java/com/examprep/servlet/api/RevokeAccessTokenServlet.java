package com.examprep.servlet.api;

import com.examprep.config.AppConfig;
import com.examprep.model.AccessGrant;
import com.examprep.service.AccessGrantService;
import com.examprep.util.SecurityUtil;
import com.examprep.util.SimpleJson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet("/api/access-tokens/revoke")
public class RevokeAccessTokenServlet extends HttpServlet {

    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAuthorized(req)) {
            writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    SimpleJson.object("error", "Invalid or missing API key"));
            return;
        }

        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Long id = SimpleJson.longField(body, "id").orElse(null);
        if (id == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    SimpleJson.object("error", "id is required"));
            return;
        }

        try {
            AccessGrant grant = accessGrantService.revoke(id);
            writeJson(resp, HttpServletResponse.SC_OK, SimpleJson.object(
                    "id", String.valueOf(grant.getId()),
                    "status", grant.getStatus().name()
            ));
        } catch (IllegalArgumentException e) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, SimpleJson.object("error", e.getMessage()));
        } catch (Exception e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    SimpleJson.object("error", "Failed to revoke access token"));
        }
    }

    private boolean isAuthorized(HttpServletRequest req) {
        String configured = AppConfig.get("funnel.api.key", "");
        if (configured.isBlank()) {
            return false;
        }
        String provided = req.getHeader("X-Api-Key");
        return SecurityUtil.constantTimeEquals(configured, provided);
    }

    private void writeJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }
}
