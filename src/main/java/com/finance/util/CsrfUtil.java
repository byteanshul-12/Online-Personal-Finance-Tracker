package com.finance.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates, stores, and validates per-session synchronizer CSRF tokens.
 */
public final class CsrfUtil {

    public static final String SESSION_ATTR = "csrfToken";
    public static final String PARAM_NAME = "_csrf";
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    /**
     * Returns the current session's CSRF token, creating one if absent.
     */
    public static String getOrCreateToken(HttpSession session) {
        Object existing = session.getAttribute(SESSION_ATTR);
        if (existing instanceof String token && !token.isEmpty()) {
            return token;
        }
        return rotateToken(session);
    }

    /**
     * Generates and stores a fresh CSRF token on the given session.
     */
    public static String rotateToken(HttpSession session) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_ATTR, token);
        return token;
    }

    /**
     * Validates the submitted CSRF token (form parameter or request header) against the session token.
     */
    public static boolean isValidToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expectedObj = session.getAttribute(SESSION_ATTR);
        if (!(expectedObj instanceof String expected) || expected.isEmpty()) {
            return false;
        }

        String submitted = request.getParameter(PARAM_NAME);
        if (submitted == null || submitted.isEmpty()) {
            submitted = request.getHeader(HEADER_NAME);
        }
        if (submitted == null || submitted.isEmpty()) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }
}
