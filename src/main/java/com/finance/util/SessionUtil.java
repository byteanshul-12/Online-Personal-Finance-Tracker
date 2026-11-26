package com.finance.util;

import com.finance.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Session lifecycle helpers to prevent session fixation and rotate CSRF tokens on login/registration.
 */
public final class SessionUtil {

    private SessionUtil() {
    }

    /**
     * Invalidates any pre-existing session, creates a fresh session with a new ID,
     * binds the authenticated user, and rotates the CSRF token.
     */
    public static HttpSession establishAuthenticatedSession(HttpServletRequest request, User user) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("user", user);
        CsrfUtil.rotateToken(newSession);
        return newSession;
    }
}
