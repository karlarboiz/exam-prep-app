package com.examprep.servlet.api;

import com.examprep.config.AppConfig;
import com.examprep.model.AccessGrant;
import com.examprep.service.AccessGrantService;
import com.examprep.util.SimpleJson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/api/access-tokens")
public class CreateAccessTokenServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CreateAccessTokenServlet.class);

    private final AccessGrantService accessGrantService = new AccessGrantService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isAuthorized(req)) {
            log.warn("Unauthorized access-token create attempt");
            writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    SimpleJson.object("error", "Invalid or missing API key"));
            return;
        }

        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LocalDateTime expiresAt = SimpleJson.dateTimeField(body, "expiresAt").orElse(null);
        Integer durationDays = SimpleJson.intField(body, "durationDays").orElse(null);
        String planCode = SimpleJson.stringField(body, "planCode").orElse(null);
        String sourceRef = SimpleJson.stringField(body, "sourceRef").orElse(null);

        try {
            AccessGrantService.CreatedAccessToken created =
                    accessGrantService.createToken(expiresAt, durationDays, planCode, sourceRef);
            AccessGrant grant = created.grant();
            String expiresAtIso = grant.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            log.info("Created access token id={} planCode={} sourceRef={} expiresAt={}",
                    grant.getId(), planCode, sourceRef, expiresAtIso);
            writeJson(resp, HttpServletResponse.SC_CREATED, SimpleJson.object(
                    "token", created.rawToken(),
                    "expiresAt", expiresAtIso,
                    "id", String.valueOf(grant.getId()),
                    "status", grant.getStatus().name()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid access-token create request: {}", e.getMessage());
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST, SimpleJson.object("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create access token", e);
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    SimpleJson.object("error", "Failed to create access token"));
        }
    }

    private boolean isAuthorized(HttpServletRequest req) {
        String configured = AppConfig.get("funnel.api.key", "");
        if (configured.isBlank()) {
            return false;
        }
        String provided = req.getHeader("X-Api-Key");
        return configured.equals(provided);
    }

    private void writeJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }
}
