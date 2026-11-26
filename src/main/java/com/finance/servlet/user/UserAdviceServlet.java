package com.finance.servlet.user;

import com.finance.exception.DatabaseException;
import com.finance.exception.ValidationException;
import com.finance.model.Advice;
import com.finance.model.AdviceRequest;
import com.finance.model.User;
import com.finance.service.AdviceRequestService;
import com.finance.service.AdviceService;
import com.finance.service.AiAdvisorService;
import com.finance.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller servlet for normal users to chat with the AI financial advisor,
 * submit advice requests to human advisors, and view received advice.
 */
@WebServlet(urlPatterns = {"/user/advice", "/user/advice/ask-ai", "/user/advice/request-advisor"})
public class UserAdviceServlet extends HttpServlet {

    private static final String CHAT_SESSION_KEY = "aiChatHistory";

    private AiAdvisorService aiAdvisorService;
    private AdviceRequestService adviceRequestService;
    private AdviceService adviceService;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        this.aiAdvisorService = new AiAdvisorService();
        this.adviceRequestService = new AdviceRequestService();
        this.adviceService = new AdviceService();
        this.userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Object flashSuccess = session.getAttribute("adviceSuccessMessage");
        if (flashSuccess != null) {
            request.setAttribute("successMessage", flashSuccess.toString());
            session.removeAttribute("adviceSuccessMessage");
        }

        try {
            List<AdviceRequest> myRequests = adviceRequestService.getRequestsByUser(user.getId());
            List<Advice> receivedAdvice = adviceService.getAdviceForUser(user.getId());
            List<User> allUsers = userService.getAllUsers();
            List<User> advisors = new ArrayList<>();
            for (User u : allUsers) {
                if ("ADVISOR".equalsIgnoreCase(u.getRole())) {
                    advisors.add(u);
                }
            }

            request.setAttribute("myRequests", myRequests);
            request.setAttribute("receivedAdvice", receivedAdvice);
            request.setAttribute("advisors", advisors);
            request.setAttribute("chatHistory", getOrCreateChatHistory(session));
            request.getRequestDispatcher("/WEB-INF/views/user/advice.jsp").forward(request, response);

        } catch (DatabaseException e) {
            request.setAttribute("errorMessage", "Failed to load advice center: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/user/advice.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String path = request.getServletPath();

        if ("/user/advice/ask-ai".equals(path)) {
            handleAskAi(request, response, session, user);
            return;
        }

        if ("/user/advice/request-advisor".equals(path)) {
            handleRequestAdvisor(request, response, session, user);
        }
    }

    private void handleAskAi(HttpServletRequest request, HttpServletResponse response,
                             HttpSession session, User user) throws IOException {
        String prompt = request.getParameter("prompt");
        if (prompt == null) {
            prompt = "";
        }
        String trimmedPrompt = prompt.trim();

        try {
            AiAdvisorService.AiReply aiReply = aiAdvisorService.ask(user, trimmedPrompt);

            List<Map<String, String>> history = getOrCreateChatHistory(session);
            if (!trimmedPrompt.isEmpty()) {
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("sender", "user");
                userMsg.put("text", trimmedPrompt);
                history.add(userMsg);
            }
            Map<String, String> botMsg = new HashMap<>();
            botMsg.put("sender", "bot");
            botMsg.put("text", aiReply.getMessage());
            botMsg.put("mode", aiReply.getMode());
            history.add(botMsg);

            String requestedWith = request.getHeader("X-Requested-With");
            if ("fetch".equalsIgnoreCase(requestedWith)) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                String json = "{\"reply\":\"" + escapeJson(aiReply.getMessage())
                        + "\",\"mode\":\"" + escapeJson(aiReply.getMode()) + "\"}";
                response.getWriter().write(json);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/user/advice");

        } catch (DatabaseException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Unable to analyze your financial data right now.\"}");
        }
    }

    private void handleRequestAdvisor(HttpServletRequest request, HttpServletResponse response,
                                      HttpSession session, User user) throws ServletException, IOException {
        String advisorId = request.getParameter("advisor_id");
        String subject = request.getParameter("subject");
        String message = request.getParameter("message");

        try {
            adviceRequestService.submitRequest(user.getId(), advisorId, subject, message);
            session.setAttribute("adviceSuccessMessage", "Your advice request has been sent to the advisor.");
            response.sendRedirect(request.getContextPath() + "/user/advice");
        } catch (ValidationException e) {
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response);
        } catch (DatabaseException e) {
            request.setAttribute("errorMessage", "Database error sending your advice request.");
            doGet(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getOrCreateChatHistory(HttpSession session) {
        Object existing = session.getAttribute(CHAT_SESSION_KEY);
        if (existing instanceof List<?>) {
            return (List<Map<String, String>>) existing;
        }
        List<Map<String, String>> history = new ArrayList<>();
        Map<String, String> welcome = new HashMap<>();
        welcome.put("sender", "bot");
        welcome.put("mode", "local");
        welcome.put("text", "Hi! I'm your AI Financial Assistant. Ask me about your spending, budgets, or how to save more—or send a request to a human advisor on the right.");
        history.add(welcome);
        session.setAttribute(CHAT_SESSION_KEY, history);
        return history;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
