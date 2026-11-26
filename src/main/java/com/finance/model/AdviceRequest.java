package com.finance.model;

import java.io.Serializable;
import java.sql.Date;

/**
 * Represents a normal user's request for financial advice from a human advisor.
 */
public class AdviceRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String userId;
    private String userName;
    private String advisorId;
    private String advisorName;
    private String subject;
    private String message;
    private String status; // OPEN or ANSWERED
    private Date createdAt;

    public AdviceRequest() {
    }

    public AdviceRequest(String id, String userId, String advisorId, String subject,
                         String message, String status, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.advisorId = advisorId;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAdvisorId() {
        return advisorId;
    }

    public void setAdvisorId(String advisorId) {
        this.advisorId = advisorId;
    }

    public String getAdvisorName() {
        return advisorName;
    }

    public void setAdvisorName(String advisorName) {
        this.advisorName = advisorName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
