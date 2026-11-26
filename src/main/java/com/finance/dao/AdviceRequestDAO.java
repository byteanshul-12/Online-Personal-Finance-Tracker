package com.finance.dao;

import com.finance.DBConnection;
import com.finance.exception.DatabaseException;
import com.finance.model.AdviceRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user-submitted advice requests to human advisors.
 */
public class AdviceRequestDAO {

    private static volatile boolean tableChecked = false;

    /**
     * Ensures the advice_requests table exists so new deployments work seamlessly.
     */
    private void ensureTableExists(Connection conn) throws SQLException {
        if (tableChecked) {
            return;
        }
        String ddl = "CREATE TABLE IF NOT EXISTS advice_requests ("
                + "id VARCHAR(30) PRIMARY KEY, "
                + "user_id VARCHAR(30) NOT NULL, "
                + "advisor_id VARCHAR(30) NULL, "
                + "subject VARCHAR(80) NOT NULL, "
                + "message VARCHAR(300) NOT NULL, "
                + "status VARCHAR(15) NOT NULL DEFAULT 'OPEN', "
                + "created_at DATE NOT NULL, "
                + "INDEX idx_adv_req_user (user_id), "
                + "INDEX idx_adv_req_advisor (advisor_id), "
                + "CONSTRAINT fk_adv_req_user FOREIGN KEY (user_id) REFERENCES user (id)"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
            tableChecked = true;
        }
    }

    public boolean createRequest(AdviceRequest req) throws DatabaseException {
        String sql = "INSERT INTO advice_requests (id, user_id, advisor_id, subject, message, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            ensureTableExists(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, req.getId());
                ps.setString(2, req.getUserId());
                if (req.getAdvisorId() != null && !req.getAdvisorId().isEmpty()) {
                    ps.setString(3, req.getAdvisorId());
                } else {
                    ps.setNull(3, Types.VARCHAR);
                }
                ps.setString(4, req.getSubject());
                ps.setString(5, req.getMessage());
                ps.setString(6, req.getStatus());
                ps.setDate(7, req.getCreatedAt());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to submit advice request: " + e.getMessage(), e);
        }
    }

    public List<AdviceRequest> findByUserId(String userId) throws DatabaseException {
        String sql = "SELECT r.id, r.user_id, u.name AS user_name, r.advisor_id, a.name AS advisor_name, "
                + "r.subject, r.message, r.status, r.created_at "
                + "FROM advice_requests r "
                + "JOIN user u ON r.user_id = u.id "
                + "LEFT JOIN user a ON r.advisor_id = a.id "
                + "WHERE r.user_id = ? "
                + "ORDER BY r.created_at DESC, r.id DESC";
        List<AdviceRequest> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            ensureTableExists(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(extractRow(rs));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error loading user advice requests: " + e.getMessage(), e);
        }
    }

    /**
     * Returns open advice requests targeted to this advisor or open to any advisor.
     */
    public List<AdviceRequest> findOpenForAdvisor(String advisorId) throws DatabaseException {
        String sql = "SELECT r.id, r.user_id, u.name AS user_name, r.advisor_id, a.name AS advisor_name, "
                + "r.subject, r.message, r.status, r.created_at "
                + "FROM advice_requests r "
                + "JOIN user u ON r.user_id = u.id "
                + "LEFT JOIN user a ON r.advisor_id = a.id "
                + "WHERE r.status = 'OPEN' AND (r.advisor_id IS NULL OR r.advisor_id = ?) "
                + "ORDER BY r.created_at DESC, r.id DESC";
        List<AdviceRequest> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            ensureTableExists(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, advisorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(extractRow(rs));
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error loading open advice requests: " + e.getMessage(), e);
        }
    }

    public boolean markAnswered(String requestId) throws DatabaseException {
        String sql = "UPDATE advice_requests SET status = 'ANSWERED' WHERE id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            ensureTableExists(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, requestId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating advice request status: " + e.getMessage(), e);
        }
    }

    private AdviceRequest extractRow(ResultSet rs) throws SQLException {
        AdviceRequest req = new AdviceRequest();
        req.setId(rs.getString("id"));
        req.setUserId(rs.getString("user_id"));
        req.setUserName(rs.getString("user_name"));
        req.setAdvisorId(rs.getString("advisor_id"));
        req.setAdvisorName(rs.getString("advisor_name"));
        req.setSubject(rs.getString("subject"));
        req.setMessage(rs.getString("message"));
        req.setStatus(rs.getString("status"));
        req.setCreatedAt(rs.getDate("created_at"));
        return req;
    }
}
