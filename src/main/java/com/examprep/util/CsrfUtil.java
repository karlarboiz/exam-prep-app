package com.examprep.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF (Cross-Site Request Forgery) protection utility.
 * Generates and validates tokens to prevent unauthorized form submissions.
 */
public final class CsrfUtil {

    private static final String CSRF_TOKEN_ATTR = "_csrf_token";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    /**
     * Generates a new CSRF token and stores it in the session.
     * 
     * @param request the HTTP request
     * @return the generated CSRF token
     */
    public static String generateToken(HttpServletRequest request) {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        HttpSession session = request.getSession(true);
        session.setAttribute(CSRF_TOKEN_ATTR, token);
        
        return token;
    }

    /**
     * Gets the current CSRF token from the session, generating a new one if needed.
     * 
     * @param request the HTTP request
     * @return the CSRF token
     */
    public static String getToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
            if (token != null && !token.isBlank()) {
                return token;
            }
        }
        return generateToken(request);
    }

    /**
     * Validates a CSRF token from the request against the session token.
     * Checks both the request parameter and the X-CSRF-Token header.
     * 
     * @param request the HTTP request
     * @return true if the token is valid, false otherwise
     */
    public static boolean validateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }

        String requestToken = request.getParameter("_csrf");
        if (requestToken == null || requestToken.isBlank()) {
            requestToken = request.getHeader(CSRF_HEADER_NAME);
        }

        if (requestToken == null || requestToken.isBlank()) {
            return false;
        }

        return SecurityUtil.constantTimeEquals(sessionToken, requestToken);
    }

    /**
     * Checks if a request should be exempt from CSRF validation.
     * GET, HEAD, OPTIONS, and TRACE are safe methods that don't need CSRF protection.
     * 
     * @param request the HTTP request
     * @return true if the request is exempt from CSRF validation
     */
    public static boolean isExemptMethod(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase();
        return "GET".equals(method) || "HEAD".equals(method) 
                || "OPTIONS".equals(method) || "TRACE".equals(method);
    }
}
