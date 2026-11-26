# Online Personal Finance Tracker

A Java-based web application for managing personal finances through expense tracking, budget management, financial reports, visual analytics, advisor guidance, and feedback.

Developed as a college project to demonstrate Java programming, JDBC, MySQL, Jakarta Servlets, JSP, session management, role-based access control, exception handling, transactions, and multithreading.

## 1. Project Overview

The Online Personal Finance Tracker helps users monitor their income and expenses, manage budgets, understand spending patterns, and make informed financial decisions.

The application provides three role-based modules:

### User
- Register and log in securely.
- Add, view, update, and delete expenses.
- Create and manage budgets.
- View spending summaries and recent activity.
- Generate financial reports.
- Compare budget allocations with actual spending.
- View financial advice.
- Submit feedback.

### Advisor
- Log in through an advisor account.
- View available users.
- Access selected users' expense information.
- Provide financial advice.
- View previously submitted advice records.

### Admin
- Manage user accounts.
- View submitted feedback.
- Update feedback status.
- Monitor basic system-level statistics.

## 2. Key Features

| Feature | Description |
|---|---|
| Authentication | Registration, login, logout, and session management |
| Expense Management | Complete expense CRUD operations |
| Budget Management | Create, update, view, and delete budgets |
| Financial Reports | Spending summaries and category-wise reports |
| Graphical Analytics | Visual representation of financial and spending information |
| Budget Analysis | Compare actual expenditure against budget allocations |
| Advisor Module | Financial guidance and user expense information |
| Feedback System | Feedback submission and administrative review |
| Role-Based Access | Separate permissions for User, Advisor, and Admin |

### Graphical Financial Analytics

The application includes graphical representations to make financial information easier to understand.

The analytics section is intended to help users:
- Understand category-wise spending distribution.
- Observe monthly expenditure patterns.
- Review spending trends.
- Compare financial activity with budget allocations.

## 3. Technology Stack

| Area | Technology |
|---|---|
| Programming Language | Java |
| Frontend | JSP, HTML, CSS, JavaScript |
| Web Layer | Jakarta Servlets |
| Backend | Java Service Classes |
| Database | MySQL 8.x |
| Database Connectivity | JDBC |
| Build Tool | Apache Maven |
| Web Server | Apache Tomcat 11 |
| Java Version | JDK 21+ |
| Packaging | WAR |

## 4. Application Architecture

The project follows a layered architecture using the DAO and Service patterns.

```text
              Browser
                 |
          JSP / HTML / CSS
                 |
              Servlet
                 |
              Service
                 |
                DAO
                 |
                JDBC
                 |
               MySQL
```

| Layer | Responsibility |
|---|---|
| JSP / Frontend | Displays pages and collects user input |
| Servlets | Handles HTTP requests and responses |
| Services | Implements application and business logic |
| DAO | Handles database operations |
| Models | Represents application data |
| Filters | Handles authentication and role-based access |

## 5. Database

The application uses MySQL.

**Database Name:** `suspicious4`

The database schema is available at:

```text
database/schema.sql
```

### Main Database Tables

| Table | Purpose |
|---|---|
| `user` | User account information |
| `expenses` | Expense records |
| `budgets` | Budget information |
| `advice` | Financial advice records |
| `feedback` | User feedback |

The schema provides the database structure without requiring pre-filled demonstration records.

## 6. Installation and Setup

### Prerequisites

Install the following software:

- JDK 21 or newer
- Apache Maven
- MySQL 8.x
- MySQL Workbench
- Apache Tomcat 11
- Git

Verify Java and Maven installations:

```bash
java -version
mvn -version
```

### Step 1: Clone the Repository

```bash
git clone https://github.com/shreybaba/Online-Personal-Finance-Tracker.git
```

Navigate to the project directory:

```bash
cd Online-Personal-Finance-Tracker
```

### Step 2: Create the Database

1. Start your MySQL server.
2. Open MySQL Workbench.
3. Open `database/schema.sql`.
4. Execute the complete SQL script.

Verify the database:

```sql
SHOW DATABASES;

USE suspicious4;

SHOW TABLES;
```

### Step 3: Configure Database Credentials

Create the configuration file:

```text
src/main/resources/db.properties
```

Use `db.properties.example` as the template.

Add your local MySQL credentials:

```properties
db.url=jdbc:mysql://localhost:3306/suspicious4
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

Replace `YOUR_MYSQL_PASSWORD` with your local MySQL password.

**Configuration security:**

- Commit `db.properties.example` to GitHub.
- Keep the actual `db.properties` file local.
- Never commit database passwords or credentials.

### Step 4: Build the Application

Open a terminal in the project root and execute:

```bash
mvn clean package
```

After a successful build, Maven generates:

```text
target/finance.war
```

### Step 5: Deploy to Apache Tomcat

Locate your Tomcat installation.

Example:

```text
C:\apache-tomcat-11.0.26
```

Copy the generated WAR file:

```text
target/finance.war
```

into:

```text
C:\apache-tomcat-11.0.26\webapps\
```

Tomcat automatically extracts the WAR file during deployment.

### Step 6: Start Tomcat

Open the Tomcat `bin` directory:

```cmd
cd C:\apache-tomcat-11.0.26\bin
```

Start the server:

```cmd
startup.bat
```

### Step 7: Open the Application

Visit:

```text
http://localhost:8080/finance/
```

The application should now be accessible through your browser.

## 7. Application Testing Flow

A basic testing workflow:

```text
Registration
     |
    Login
     |
User Dashboard
     |
Add Expenses
     |
Manage Expenses
     |
Create Budget
     |
View Financial Reports
     |
Explore Graphical Analytics
```

Advisor and Admin functionality can be tested using accounts with the appropriate roles.

## 8. Project Structure

```text
Online-Personal-Finance-Tracker/
|
├── database/
│   └── schema.sql
|
├── docs/
|
├── src/
│   └── main/
│       ├── java/
│       │   └── com/finance/
│       │       ├── dao/
│       │       ├── exception/
│       │       ├── filter/
│       │       ├── model/
│       │       ├── service/
│       │       ├── servlet/
│       │       └── util/
│       │
│       ├── resources/
│       │   └── db.properties.example
│       │
│       └── webapp/
│           ├── WEB-INF/
│           ├── assets/
│           └── index.jsp
|
├── db.properties.example
├── pom.xml
├── README.md
└── .gitignore
```

The actual local database configuration file `db.properties` is excluded from version control.

## 9. Java Concepts Demonstrated

This project applies several Java programming concepts in a practical web application.

| Concept | Implementation |
|---|---|
| Object-Oriented Programming | Models, services, DAOs, inheritance, encapsulation |
| Collections | List, ArrayList, Map, HashMap |
| Exception Handling | Custom exception hierarchy |
| JDBC | Database connectivity and SQL operations |
| Transactions | Commit and rollback |
| Multithreading | ExecutorService, Callable, Future |
| HTTP | Servlets, GET/POST, forwards and redirects |
| Session Management | HttpSession and authentication filters |
| Design Patterns | DAO and Service patterns |

### Exception Hierarchy

```text
FinanceTrackerException
├── AuthenticationException
├── AuthorizationException
├── DatabaseException
└── ValidationException
```

### Multithreading

The report service uses Java concurrency utilities such as:

```java
ExecutorService
Callable
Future
```

These utilities support independent report-processing tasks.

### Transaction Management

The application demonstrates JDBC transaction management using:

```java
setAutoCommit(false);
commit();
rollback();
```

## 10. Important Project Files

| Component | Location |
|---|---|
| Database Schema | `database/schema.sql` |
| Database Connection | `src/main/java/com/finance/DBConnection.java` |
| DAO Classes | `src/main/java/com/finance/dao/` |
| Service Classes | `src/main/java/com/finance/service/` |
| Model Classes | `src/main/java/com/finance/model/` |
| Authentication | `src/main/java/com/finance/servlet/auth/` |
| Security Filters | `src/main/java/com/finance/filter/` |
| Report Service | `src/main/java/com/finance/service/ReportService.java` |
| Web Configuration | `src/main/webapp/WEB-INF/web.xml` |

## 11. Troubleshooting

### Maven Build Failure

Run:

```bash
mvn clean package
```

If Maven cannot delete the `target` directory, stop Tomcat and other processes that may be using project files before rebuilding.

### MySQL Connection Error

Verify:
- MySQL server is running.
- Database `suspicious4` exists.
- Database credentials are correct.
- `src/main/resources/db.properties` exists.
- Required database tables have been created.

### HTTP 404 Error

Verify:
- Tomcat is running.
- `finance.war` is inside Tomcat's `webapps` directory.
- Application deployment completed successfully.
- The requested servlet mapping exists.
- Check Tomcat logs for deployment errors.

### HTTP 500 Error

Check the Tomcat logs immediately after reproducing the error.

Common causes include:
- Database connection failure.
- Missing configuration files.
- Incorrect SQL queries.
- Missing dependencies.
- Application initialization errors.

## 12. Repository

**GitHub Repository:**

https://github.com/shreybaba/Online-Personal-Finance-Tracker

The repository contains the source code, database schema, configuration template, and documentation required to understand and run the application locally.

---

**Developed by Shreyansh**

*Online Personal Finance Tracker — A practical implementation of Java-based web application development and database management.*
