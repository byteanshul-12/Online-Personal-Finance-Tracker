package com.finance.servlet.user;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import com.finance.exception.DatabaseException;
import com.finance.model.User;
import com.finance.service.ReportService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * JSON API endpoint providing aggregated category and trend data for expenditure charts.
 */
@WebServlet("/user/api/expense-charts")
public class ExpenseChartServlet extends HttpServlet {

    private ReportService reportService;

    @Override
    public void init() throws ServletException {
        this.reportService = new ReportService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null || user.getId() == null || user.getId().isBlank()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\":\"Unauthorized: Active user session required.\"}");
            return;
        }

        String granularity = "daily".equalsIgnoreCase(req.getParameter("granularity"))
                ? "daily"
                : "monthly";

        try {
            Map<String, BigDecimal> byCategory = reportService.getCategorySpendingBreakdown(user.getId());
            Map<String, BigDecimal> trend = reportService.getExpenditureTrend(user.getId(), granularity);

            BigDecimal totalSpent = BigDecimal.ZERO;
            for (BigDecimal amount : byCategory.values()) {
                totalSpent = totalSpent.add(amount);
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(buildJsonPayload(granularity, totalSpent, byCategory, trend));

        } catch (DatabaseException e) {
            getServletContext().log("Database error while aggregating expenses", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error while aggregating expenses.\"}");
        }
    }

    private String buildJsonPayload(String granularity,
                                    BigDecimal totalSpent,
                                    Map<String, BigDecimal> categories,
                                    Map<String, BigDecimal> trend) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{");
        sb.append("\"granularity\":\"").append(escapeJson(granularity)).append("\",");
        sb.append("\"totalSpent\":").append(totalSpent.toPlainString()).append(",");
        sb.append("\"categories\":");
        appendSeries(sb, categories);
        sb.append(",\"trend\":");
        appendSeries(sb, trend);
        sb.append("}");
        return sb.toString();
    }

    private void appendSeries(StringBuilder sb, Map<String, BigDecimal> series) {
        sb.append("[");
        boolean first = true;
        for (Map.Entry<String, BigDecimal> entry : series.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("{\"label\":\"")
              .append(escapeJson(entry.getKey()))
              .append("\",\"amount\":")
              .append(entry.getValue().toPlainString())
              .append("}");
            first = false;
        }
        sb.append("]");
    }

    private String escapeJson(String value) {
    if (value == null) return "";
    StringBuilder out = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        switch (c) {
            case '"'  -> out.append("\\\"");
            case '\\' -> out.append("\\\\");
            case '\b' -> out.append("\\b");
            case '\f' -> out.append("\\f");
            case '\n' -> out.append("\\n");
            case '\r' -> out.append("\\r");
            case '\t' -> out.append("\\t");
            default -> {
                if (c < 0x20) {
                    out.append(String.format("\\u%04x", (int) c));
                } else {
                    out.append(c);
                }
            }
        }
    }
    return out.toString();
}
}