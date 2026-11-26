package com.finance.dao;

import com.finance.model.Advice;
import com.finance.model.Budget;
import com.finance.model.Expense;
import com.finance.model.Feedback;
import com.finance.model.User;
import com.finance.testutil.DatabaseTestSupport;
import com.finance.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseOperationsTest {

    private UserDAO userDAO;
    private ExpenseDAO expenseDAO;
    private BudgetDAO budgetDAO;
    private AdviceDAO adviceDAO;
    private FeedbackDAO feedbackDAO;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.resetSchema();
        userDAO = new UserDAO();
        expenseDAO = new ExpenseDAO();
        budgetDAO = new BudgetDAO();
        adviceDAO = new AdviceDAO();
        feedbackDAO = new FeedbackDAO();
    }

    @Test
    void expenseDaoCrudOwnershipAndAggregationsWork() throws Exception {
        userDAO.createUser(new User("USR_1", "User One", "USER", PasswordUtil.hash("pass1234"), "u1@example.com"));
        userDAO.createUser(new User("USR_2", "User Two", "USER", PasswordUtil.hash("pass1234"), "u2@example.com"));

        Date today = Date.valueOf(LocalDate.now());
        Date earlier = Date.valueOf(LocalDate.now().minusDays(5));

        assertTrue(expenseDAO.createExpense(new Expense("EXP_1", "Groceries", new BigDecimal("120.50"), today, "USR_1")));
        assertTrue(expenseDAO.createExpense(new Expense("EXP_2", "Rent", new BigDecimal("800.00"), earlier, "USR_1")));
        assertTrue(expenseDAO.createExpense(new Expense("EXP_3", "Groceries", new BigDecimal("50.00"), today, "USR_2")));

        // Ownership enforcement on update/delete
        Expense spoofedUpdate = new Expense("EXP_1", "Hacked", new BigDecimal("1.00"), today, "USR_2");
        assertFalse(expenseDAO.updateExpense(spoofedUpdate));
        assertFalse(expenseDAO.deleteExpense("EXP_1", "USR_2"));

        assertEquals(0, new BigDecimal("920.50").compareTo(expenseDAO.getTotalExpensesByUserId("USR_1")));

        Map<String, BigDecimal> byCat = expenseDAO.getCategorySpendingTotals("USR_1");
        assertEquals(0, new BigDecimal("800.00").compareTo(byCat.get("Rent")));
        assertEquals(0, new BigDecimal("120.50").compareTo(byCat.get("Groceries")));

        Map<String, BigDecimal> dailyTrend = expenseDAO.getSpendingTrend("USR_1", "daily");
        assertFalse(dailyTrend.isEmpty());

        List<Expense> filtered = expenseDAO.findByFilter("USR_1", earlier, today, "Rent");
        assertEquals(1, filtered.size());
        assertEquals("EXP_2", filtered.get(0).getId());
    }

    @Test
    void budgetAdviceAndFeedbackDaosPersistAndQueryRecords() throws Exception {
        userDAO.createUser(new User("USR_A", "Client", "USER", PasswordUtil.hash("pass1234"), "client@example.com"));
        userDAO.createUser(new User("ADV_1", "Advisor", "ADVISOR", PasswordUtil.hash("pass1234"), "adv@example.com"));

        Date today = Date.valueOf(LocalDate.now());

        assertTrue(budgetDAO.saveOrUpdateBudget(new Budget("BGT_1", "USR_A", "Food", new BigDecimal("500.00"), "Monthly")));
        // Updating same category overwrites limit rather than duplicating
        assertTrue(budgetDAO.saveOrUpdateBudget(new Budget("BGT_2", "USR_A", "Food", new BigDecimal("650.00"), "Monthly")));
        assertEquals(1, budgetDAO.findByUserId("USR_A").size());
        assertEquals(0, new BigDecimal("650.00").compareTo(budgetDAO.getTotalBudgetByUserId("USR_A")));

        assertTrue(adviceDAO.createAdvice(new Advice("ADV_MSG_1", "ADV_1", "Track groceries weekly", today, "USR_A")));
        assertEquals(1, adviceDAO.findByUserId("USR_A").size());
        assertEquals(1, adviceDAO.countUniqueUsersAdvised("ADV_1"));

        assertTrue(feedbackDAO.createFeedback(new Feedback("FBK_1", "USR_A", "Great tracker!", "PENDING", today)));
        assertEquals(1, feedbackDAO.countPendingFeedback());
        assertTrue(feedbackDAO.updateStatus("FBK_1", "RESOLVED"));
        assertEquals(0, feedbackDAO.countPendingFeedback());
    }

    @Test
    void deleteUserWithCascadeRemovesUserAndAllDependentRowsTransactionally() throws Exception {
        userDAO.createUser(new User("USR_DEL", "Delete Me", "USER", PasswordUtil.hash("pass1234"), "del@example.com"));
        userDAO.createUser(new User("ADV_DEL", "Advisor", "ADVISOR", PasswordUtil.hash("pass1234"), "advdel@example.com"));

        Date today = Date.valueOf(LocalDate.now());
        expenseDAO.createExpense(new Expense("EXP_D1", "Food", new BigDecimal("45.00"), today, "USR_DEL"));
        budgetDAO.saveOrUpdateBudget(new Budget("BGT_D1", "USR_DEL", "Food", new BigDecimal("200.00"), "Monthly"));
        adviceDAO.createAdvice(new Advice("ADV_D1", "ADV_DEL", "Reduce takeout", today, "USR_DEL"));
        feedbackDAO.createFeedback(new Feedback("FBK_D1", "USR_DEL", "Need dark charts", "PENDING", today));

        assertTrue(userDAO.deleteUserWithCascade("USR_DEL"));

        assertNull(userDAO.findById("USR_DEL"));
        assertTrue(expenseDAO.findByUserId("USR_DEL").isEmpty());
        assertTrue(budgetDAO.findByUserId("USR_DEL").isEmpty());
        assertTrue(adviceDAO.findByUserId("USR_DEL").isEmpty());
        assertTrue(feedbackDAO.findAllFeedback().isEmpty());
        assertEquals(1, userDAO.countUsers());
    }
}
