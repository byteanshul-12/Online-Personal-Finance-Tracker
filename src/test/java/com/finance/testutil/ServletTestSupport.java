package com.finance.testutil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight dynamic-proxy helpers for testing Servlets and Filters without bytecode agents.
 */
public final class ServletTestSupport {

    private static final AtomicInteger SESSION_SEQ = new AtomicInteger(1);

    private ServletTestSupport() {
    }

    public static final class RecordedResponse {
        public int status = 200;
        public String redirectLocation;
        public String errorMessage;
        public String forwardedPath;
        public String contentType;
        public final StringWriter body = new StringWriter();
        public final PrintWriter writer = new PrintWriter(body);
    }

    public static final class RecordedChain {
        public boolean invoked;

        public FilterChain asFilterChain() {
            return (req, res) -> invoked = true;
        }
    }

    public static HttpSession createSession() {
        return createSession("SESSION-" + SESSION_SEQ.getAndIncrement());
    }

    public static HttpSession createSession(String sessionId) {
        Map<String, Object> attributes = new HashMap<>();
        boolean[] invalidated = {false};

        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> sessionId;
                    case "getAttribute" -> {
                        if (invalidated[0]) throw new IllegalStateException("Session invalidated");
                        yield attributes.get(args[0]);
                    }
                    case "setAttribute" -> {
                        if (invalidated[0]) throw new IllegalStateException("Session invalidated");
                        attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "removeAttribute" -> {
                        attributes.remove(args[0]);
                        yield null;
                    }
                    case "invalidate" -> {
                        invalidated[0] = true;
                        attributes.clear();
                        yield null;
                    }
                    case "getAttributeNames" -> Collections.enumeration(attributes.keySet());
                    default -> defaultReturn(method.getReturnType());
                });
    }

    public static final class RequestBuilder {
        private String method = "GET";
        private String contextPath = "/finance";
        private String servletPath = "";
        private String pathInfo = null;
        private String requestUri = "/finance/";
        private HttpSession session;
        private final Map<String, String> params = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();

        public RequestBuilder method(String method) {
            this.method = method;
            return this;
        }

        public RequestBuilder path(String servletPath) {
            this.servletPath = servletPath;
            this.requestUri = this.contextPath + servletPath;
            return this;
        }

        public RequestBuilder session(HttpSession session) {
            this.session = session;
            return this;
        }

        public RequestBuilder param(String name, String value) {
            this.params.put(name, value);
            return this;
        }

        public RequestBuilder header(String name, String value) {
            this.headers.put(name.toLowerCase(), value);
            return this;
        }

        public HttpSession currentSession() {
            return session;
        }

        public HttpServletRequest build(RecordedResponse recorded) {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getMethod" -> method;
                        case "getContextPath" -> contextPath;
                        case "getServletPath" -> servletPath;
                        case "getPathInfo" -> pathInfo;
                        case "getRequestURI" -> requestUri;
                        case "getParameter" -> params.get(args[0]);
                        case "getHeader" -> headers.get(((String) args[0]).toLowerCase());
                        case "getAttribute" -> attributes.get(args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getSession" -> {
                            if (args == null || args.length == 0) {
                                if (session == null) session = createSession();
                                yield session;
                            }
                            boolean create = (Boolean) args[0];
                            if (create) {
                                // Mirror container creating a fresh session if absent or invalidated
                                boolean needsNew = session == null;
                                if (!needsNew) {
                                    try {
                                        session.getAttribute("__probe");
                                    } catch (IllegalStateException ex) {
                                        needsNew = true;
                                    }
                                }
                                if (needsNew) {
                                    session = createSession();
                                }
                                yield session;
                            } else {
                                if (session == null) yield null;
                                try {
                                    session.getAttribute("__probe");
                                    yield session;
                                } catch (IllegalStateException ex) {
                                    yield null;
                                }
                            }
                        }
                        case "getRequestDispatcher" -> {
                            String target = (String) args[0];
                            yield (RequestDispatcher) Proxy.newProxyInstance(
                                    RequestDispatcher.class.getClassLoader(),
                                    new Class<?>[]{RequestDispatcher.class},
                                    (dp, dm, dargs) -> {
                                        if ("forward".equals(dm.getName())) {
                                            recorded.forwardedPath = target;
                                        }
                                        return null;
                                    });
                        }
                        default -> defaultReturn(m.getReturnType());
                    });
        }
    }

    public static HttpServletResponse createResponse(RecordedResponse recorded) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendRedirect" -> {
                        recorded.status = 302;
                        recorded.redirectLocation = (String) args[0];
                        yield null;
                    }
                    case "sendError" -> {
                        recorded.status = (Integer) args[0];
                        recorded.errorMessage = args.length > 1 ? (String) args[1] : null;
                        yield null;
                    }
                    case "setStatus" -> {
                        recorded.status = (Integer) args[0];
                        yield null;
                    }
                    case "setContentType" -> {
                        recorded.contentType = (String) args[0];
                        yield null;
                    }
                    case "getWriter" -> recorded.writer;
                    default -> defaultReturn(method.getReturnType());
                });
    }

    public static ServletConfig createServletConfig() {
        ServletContext ctx = (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class},
                (proxy, method, args) -> defaultReturn(method.getReturnType()));
        return (ServletConfig) Proxy.newProxyInstance(
                ServletConfig.class.getClassLoader(),
                new Class<?>[]{ServletConfig.class},
                (proxy, method, args) -> "getServletContext".equals(method.getName()) ? ctx : defaultReturn(method.getReturnType()));
    }

    private static Object defaultReturn(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }
}
