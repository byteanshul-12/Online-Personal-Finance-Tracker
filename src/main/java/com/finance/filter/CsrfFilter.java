package com.finance.filter;

import com.finance.util.CsrfUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Synchronizer-token CSRF protection filter for all state-changing HTTP requests.
 */
public class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        if (method != null && SAFE_METHODS.contains(method.toUpperCase())) {
            String path = resolvePath(httpRequest);
            if (!path.startsWith("/assets/") && !"/favicon.ico".equals(path)) {
                CsrfUtil.getOrCreateToken(httpRequest.getSession(true));
            }
            chain.doFilter(request, response);
            return;
        }

        if (!CsrfUtil.isValidToken(httpRequest)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token.");
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

    @Override
    public void destroy() {
    }
}
