package com.finance.service;

import com.finance.dao.AdviceRequestDAO;
import com.finance.dao.UserDAO;
import com.finance.exception.DatabaseException;
import com.finance.exception.ValidationException;
import com.finance.model.AdviceRequest;
import com.finance.model.User;
import com.finance.util.IDGenerator;
import com.finance.util.ValidationUtil;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Service managing advice requests sent by normal users to human financial advisors.
 */
public class AdviceRequestService {

    private final AdviceRequestDAO requestDAO;
    private final UserDAO userDAO;

    public AdviceRequestService() {
        this.requestDAO = new AdviceRequestDAO();
        this.userDAO = new UserDAO();
    }

    public AdviceRequestService(AdviceRequestDAO requestDAO, UserDAO userDAO) {
        this.requestDAO = requestDAO;
        this.userDAO = userDAO;
    }

    public void submitRequest(String userId, String advisorId, String subject, String message)
            throws ValidationException, DatabaseException {

        if (ValidationUtil.isEmpty(userId)) {
            throw new ValidationException("User session expired. Please sign in again.");
        }
        if (ValidationUtil.isEmpty(subject) || subject.trim().length() > 80) {
            throw new ValidationException("Subject is required and must not exceed 80 characters.");
        }
        if (ValidationUtil.isEmpty(message) || message.trim().length() > 300) {
            throw new ValidationException("Request message is required and must not exceed 300 characters.");
        }

        String normalizedAdvisorId = null;
        if (!ValidationUtil.isEmpty(advisorId)) {
            normalizedAdvisorId = advisorId.trim();
            User advisor = userDAO.findById(normalizedAdvisorId);
            if (advisor == null || (!"ADVISOR".equalsIgnoreCase(advisor.getRole()) && !"ADMIN".equalsIgnoreCase(advisor.getRole()))) {
                throw new ValidationException("Selected advisor account was not found.");
            }
        }

        String requestId = IDGenerator.generateId("REQ");
        AdviceRequest req = new AdviceRequest(
                requestId,
                userId,
                normalizedAdvisorId,
                subject.trim(),
                message.trim(),
                "OPEN",
                Date.valueOf(LocalDate.now())
        );
        requestDAO.createRequest(req);
    }

    public List<AdviceRequest> getRequestsByUser(String userId) throws DatabaseException {
        return requestDAO.findByUserId(userId);
    }

    public List<AdviceRequest> getOpenRequestsForAdvisor(String advisorId) throws DatabaseException {
        return requestDAO.findOpenForAdvisor(advisorId);
    }

    public void markAnswered(String requestId) throws DatabaseException {
        if (!ValidationUtil.isEmpty(requestId)) {
            requestDAO.markAnswered(requestId.trim());
        }
    }
}
