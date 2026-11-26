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
 * Filter enforcing Role-Based Access Control (RBAC) across system routes.
 * Ensures users with role USER cannot access ADVISOR/ADMIN pages, and vice versa (ordered via web.xml).
 */
public class RoleFilter implements Filter {

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
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        String path = resolvePath(httpRequest);
        String role = loggedInUser.getRole();

        // Enforce role boundaries
        if (isRoute(path, "/admin") && !"ADMIN".equalsIgnoreCase(role)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required.");
            return;
        }

        if (isRoute(path, "/advisor") && !"ADVISOR".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Advisor role required.");
            return;
        }

        if ((isRoute(path, "/user") || isRoute(path, "/feedback")) && !"USER".equalsIgnoreCase(role)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: User role required.");
            return;
        }

        chain.doFilter(request, response);
    }

    private static String resolvePath(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        if (servletPath != null && !servletPath.isEmpty()) {
            return servletPath + (pathInfo != null ? pathInfo : "");
        }
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        if (uri != null && ctx != null && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri != null ? uri : "";
    }

    private static boolean isRoute(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    @Override
    public void destroy() {
    }
}
