package com.finance.servlet;

import com.finance.filter.AuthFilter;
import com.finance.filter.CsrfFilter;
import com.finance.filter.RoleFilter;
import com.finance.model.User;
import com.finance.service.UserService;
import com.finance.servlet.auth.LoginServlet;
import com.finance.servlet.auth.LogoutServlet;
import com.finance.servlet.auth.RegisterServlet;
import com.finance.testutil.DatabaseTestSupport;
import com.finance.testutil.ServletTestSupport;
import com.finance.testutil.ServletTestSupport.RecordedChain;
import com.finance.testutil.ServletTestSupport.RecordedResponse;
import com.finance.testutil.ServletTestSupport.RequestBuilder;
import com.finance.util.CsrfUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthAndSecurityTest {

    private UserService userService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.resetSchema();
        userService = new UserService();
    }

    @Test
    void loginServletRegeneratesSessionIdAndRotatesCsrfToken() throws Exception {
        userService.register("Test User", "user@example.com", "Secret12", "USER");

        HttpSession preLoginSession = ServletTestSupport.createSession("PRE-LOGIN-ID");
        preLoginSession.setAttribute("staleAttr", "should-be-cleared");
        String oldCsrf = CsrfUtil.getOrCreateToken(preLoginSession);

        LoginServlet servlet = new LoginServlet();
        servlet.init(ServletTestSupport.createServletConfig());

        RecordedResponse respRec = new RecordedResponse();
        RequestBuilder reqBuilder = new RequestBuilder()
                .method("POST")
                .path("/login")
                .session(preLoginSession)
                .param("email", "user@example.com")
                .param("password", "Secret12");
        HttpServletRequest req = reqBuilder.build(respRec);
        HttpServletResponse resp = ServletTestSupport.createResponse(respRec);

        servlet.service(req, resp);

        assertEquals(302, respRec.status);
        assertEquals("/finance/user/dashboard", respRec.redirectLocation);

        HttpSession postLoginSession = req.getSession(false);
        assertNotNull(postLoginSession);
        assertNotEquals("PRE-LOGIN-ID", postLoginSession.getId(), "Session ID must be regenerated after login");
        assertNull(postLoginSession.getAttribute("staleAttr"));
        assertNotNull(postLoginSession.getAttribute("user"));

        String newCsrf = (String) postLoginSession.getAttribute(CsrfUtil.SESSION_ATTR);
        assertNotNull(newCsrf);
        assertNotEquals(oldCsrf, newCsrf, "CSRF token should rotate on login");
    }

    @Test
    void registerServletRegeneratesSessionId() throws Exception {
        HttpSession preSession = ServletTestSupport.createSession("PRE-REG-ID");

        RegisterServlet servlet = new RegisterServlet();
        servlet.init(ServletTestSupport.createServletConfig());

        RecordedResponse respRec = new RecordedResponse();
        HttpServletRequest req = new RequestBuilder()
                .method("POST")
                .path("/register")
                .session(preSession)
                .param("name", "New User")
                .param("email", "newuser@example.com")
                .param("password", "Pass9876")
                .param("role", "USER")
                .build(respRec);

        servlet.service(req, ServletTestSupport.createResponse(respRec));

        assertEquals(302, respRec.status);
        HttpSession postSession = req.getSession(false);
        assertNotNull(postSession);
        assertNotEquals("PRE-REG-ID", postSession.getId());
        assertNotNull(postSession.getAttribute("user"));
    }

    @Test
    void logoutServletRequiresPostAndInvalidatesSession() throws Exception {
        LogoutServlet servlet = new LogoutServlet();
        servlet.init(ServletTestSupport.createServletConfig());

        RecordedResponse getResp = new RecordedResponse();
        HttpServletRequest getReq = new RequestBuilder().method("GET").path("/logout").build(getResp);
        servlet.service(getReq, ServletTestSupport.createResponse(getResp));
        assertEquals(405, getResp.status);

        HttpSession session = ServletTestSupport.createSession();
        session.setAttribute("user", new User("USR_1", "U", "USER", "h", "u@e.com"));

        RecordedResponse postResp = new RecordedResponse();
        HttpServletRequest postReq = new RequestBuilder().method("POST").path("/logout").session(session).build(postResp);
        servlet.service(postReq, ServletTestSupport.createResponse(postResp));
        assertEquals(302, postResp.status);
        assertNull(postReq.getSession(false));
    }

    @Test
    void csrfFilterBlocksMissingOrInvalidTokenAndAllowsValidParamOrHeader() throws Exception {
        CsrfFilter filter = new CsrfFilter();

        // 1. GET generates token
        HttpSession session = ServletTestSupport.createSession();
        RecordedResponse getRec = new RecordedResponse();
        RecordedChain getChain = new RecordedChain();
        filter.doFilter(
                new RequestBuilder().method("GET").path("/login").session(session).build(getRec),
                ServletTestSupport.createResponse(getRec),
                getChain.asFilterChain());
        assertTrue(getChain.invoked);
        String token = (String) session.getAttribute(CsrfUtil.SESSION_ATTR);
        assertNotNull(token);

        // 2. POST without token -> 403
        RecordedResponse missingRec = new RecordedResponse();
        RecordedChain missingChain = new RecordedChain();
        filter.doFilter(
                new RequestBuilder().method("POST").path("/user/add-expense").session(session).build(missingRec),
                ServletTestSupport.createResponse(missingRec),
                missingChain.asFilterChain());
        assertFalse(missingChain.invoked);
        assertEquals(403, missingRec.status);

        // 3. POST with wrong token -> 403
        RecordedResponse wrongRec = new RecordedResponse();
        RecordedChain wrongChain = new RecordedChain();
        filter.doFilter(
                new RequestBuilder().method("POST").path("/user/add-expense").session(session)
                        .param(CsrfUtil.PARAM_NAME, "bad-token").build(wrongRec),
                ServletTestSupport.createResponse(wrongRec),
                wrongChain.asFilterChain());
        assertFalse(wrongChain.invoked);
        assertEquals(403, wrongRec.status);

        // 4. POST with valid form parameter -> allowed
        RecordedResponse okParamRec = new RecordedResponse();
        RecordedChain okParamChain = new RecordedChain();
        filter.doFilter(
                new RequestBuilder().method("POST").path("/user/add-expense").session(session)
                        .param(CsrfUtil.PARAM_NAME, token).build(okParamRec),
                ServletTestSupport.createResponse(okParamRec),
                okParamChain.asFilterChain());
        assertTrue(okParamChain.invoked);

        // 5. POST with valid header -> allowed
        RecordedResponse okHeaderRec = new RecordedResponse();
        RecordedChain okHeaderChain = new RecordedChain();
        filter.doFilter(
                new RequestBuilder().method("POST").path("/user/delete-expense").session(session)
                        .header(CsrfUtil.HEADER_NAME, token).build(okHeaderRec),
                ServletTestSupport.createResponse(okHeaderRec),
                okHeaderChain.asFilterChain());
        assertTrue(okHeaderChain.invoked);
    }

    @Test
    void authAndRoleFiltersEnforceAuthenticationAndStrictRoleBoundaries() throws Exception {
        AuthFilter authFilter = new AuthFilter();
        RoleFilter roleFilter = new RoleFilter();

        // Unauthenticated protected route redirects to login; API returns 401
        assertAuthRedirect(authFilter, "/user/dashboard");
        assertAuthRedirect(authFilter, "/feedback/submit");
        assertAuthRedirect(authFilter, "/advisor/dashboard");
        assertAuthRedirect(authFilter, "/admin/dashboard");

        RecordedResponse apiResp = new RecordedResponse();
        RecordedChain apiChain = new RecordedChain();
        authFilter.doFilter(
                new RequestBuilder().method("GET").path("/user/api/expense-charts").build(apiResp),
                ServletTestSupport.createResponse(apiResp),
                apiChain.asFilterChain());
        assertFalse(apiChain.invoked);
        assertEquals(401, apiResp.status);

        // USER role boundaries
        assertRoleAllowed(roleFilter, "USER", "/user/dashboard", true);
        assertRoleAllowed(roleFilter, "USER", "/feedback/submit", true);
        assertRoleAllowed(roleFilter, "USER", "/advisor/dashboard", false);
        assertRoleAllowed(roleFilter, "USER", "/admin/dashboard", false);

        // ADVISOR role boundaries
        assertRoleAllowed(roleFilter, "ADVISOR", "/advisor/dashboard", true);
        assertRoleAllowed(roleFilter, "ADVISOR", "/admin/dashboard", false);
        assertRoleAllowed(roleFilter, "ADVISOR", "/user/dashboard", false);
        assertRoleAllowed(roleFilter, "ADVISOR", "/feedback/submit", false);

        // ADMIN role boundaries
        assertRoleAllowed(roleFilter, "ADMIN", "/admin/dashboard", true);
        assertRoleAllowed(roleFilter, "ADMIN", "/advisor/dashboard", true);
        assertRoleAllowed(roleFilter, "ADMIN", "/user/dashboard", false);
        assertRoleAllowed(roleFilter, "ADMIN", "/feedback/submit", false);
    }

    private static void assertAuthRedirect(AuthFilter filter, String path) throws Exception {
        RecordedResponse rec = new RecordedResponse();
        RecordedChain chain = new RecordedChain();
        filter.doFilter(
                new RequestBuilder().method("GET").path(path).build(rec),
                ServletTestSupport.createResponse(rec),
                chain.asFilterChain());
        assertFalse(chain.invoked, "Unauthenticated request to " + path + " must be blocked");
        assertEquals(302, rec.status);
        assertEquals("/finance/login", rec.redirectLocation);
    }

    private static void assertRoleAllowed(RoleFilter filter, String role, String path, boolean expectedAllowed)
            throws Exception {
        HttpSession session = ServletTestSupport.createSession();
        session.setAttribute("user", new User("USR_TEST", "Tester", role, "hash", "t@example.com"));

        RecordedResponse rec = new RecordedResponse();
        RecordedChain chain = new RecordedChain();
        filter.doFilter(
                new RequestBuilder().method("GET").path(path).session(session).build(rec),
                ServletTestSupport.createResponse(rec),
                chain.asFilterChain());

        assertEquals(expectedAllowed, chain.invoked,
                "Role " + role + " on " + path + " allowed=" + expectedAllowed);
        if (!expectedAllowed) {
            assertEquals(403, rec.status);
        }
    }
}
