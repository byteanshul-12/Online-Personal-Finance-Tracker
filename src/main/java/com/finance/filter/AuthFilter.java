package com.finance.filter;

import com.finance.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter ensuring session validation across protected routes.
 * Intercepts requests to /user/*, /advisor/*, /admin/*, and /feedback/* endpoints (ordered via web.xml).
 */
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        User loggedInUser = (session != null) ? (User) session.getAttribute("user") : null;

        if (loggedInUser == null) {
            String uri = httpRequest.getRequestURI();
            String ctx = httpRequest.getContextPath();
            String path = (uri != null && ctx != null && uri.startsWith(ctx))
                    ? uri.substring(ctx.length())
                    : "";
            if (path.startsWith("/user/api/") || path.startsWith("/api/")) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json; charset=UTF-8");
                httpResponse.getWriter().write("{\"error\":\"Unauthorized: Active session required.\"}");
                return;
            }
            // User session is not active; redirect to login page
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        // Session valid, proceed with request execution
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
