package com.finance.testutil;

import com.finance.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;

/**
 * Initializes and resets the in-memory H2 schema for automated DAO and service tests.
 */
public final class DatabaseTestSupport {

    private DatabaseTestSupport() {
    }

    public static void resetSchema() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {

            st.execute("DROP TABLE IF EXISTS feedback");
            st.execute("DROP TABLE IF EXISTS advice");
            st.execute("DROP TABLE IF EXISTS budgets");
            st.execute("DROP TABLE IF EXISTS expenses");
            st.execute("DROP TABLE IF EXISTS `user`");

            st.execute("CREATE TABLE `user` ("
                    + "id VARCHAR(30) PRIMARY KEY, "
                    + "name VARCHAR(30) NOT NULL, "
                    + "role VARCHAR(10) NOT NULL, "
                    + "password VARCHAR(100) NOT NULL, "
                    + "email VARCHAR(50) NOT NULL UNIQUE)");

            st.execute("CREATE TABLE expenses ("
                    + "id VARCHAR(30) PRIMARY KEY, "
                    + "category VARCHAR(15) NOT NULL, "
                    + "amount DECIMAL(10,2) NOT NULL, "
                    + "`date` DATE NOT NULL, "
                    + "user_id VARCHAR(30) NOT NULL, "
                    + "CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES `user`(id))");

            st.execute("CREATE TABLE budgets ("
                    + "id VARCHAR(30) PRIMARY KEY, "
                    + "user_id VARCHAR(30) NOT NULL, "
                    + "category VARCHAR(20) NOT NULL, "
                    + "amount DECIMAL(10,2) NOT NULL, "
                    + "period VARCHAR(20) NOT NULL, "
                    + "CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES `user`(id))");

            st.execute("CREATE TABLE advice ("
                    + "id VARCHAR(30) PRIMARY KEY, "
                    + "advisor_id VARCHAR(30) NOT NULL, "
                    + "message VARCHAR(200) NOT NULL, "
                    + "`date` DATE NOT NULL, "
                    + "user_id VARCHAR(30) NOT NULL, "
                    + "CONSTRAINT fk_advice_advisor FOREIGN KEY (advisor_id) REFERENCES `user`(id), "
                    + "CONSTRAINT fk_advice_user FOREIGN KEY (user_id) REFERENCES `user`(id))");

            st.execute("CREATE TABLE feedback ("
                    + "id VARCHAR(30) PRIMARY KEY, "
                    + "user_id VARCHAR(30) NOT NULL, "
                    + "message VARCHAR(100) NOT NULL, "
                    + "status VARCHAR(100) NOT NULL, "
                    + "`date` DATE NOT NULL, "
                    + "CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES `user`(id))");

            st.execute("CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR \"com.finance.testutil.DatabaseTestSupport.dateFormat\"");
        }
    }

    /**
     * H2 SQL function alias mirroring MySQL DATE_FORMAT(date, pattern) for '%Y-%m' and '%Y-%m-%d'.
     */
    public static String dateFormat(Date date, String mysqlPattern) {
        if (date == null) {
            return null;
        }
        String javaPattern = "%Y-%m-%d".equals(mysqlPattern) ? "yyyy-MM-dd" : "yyyy-MM";
        return date.toLocalDate().format(DateTimeFormatter.ofPattern(javaPattern));
    }
}
