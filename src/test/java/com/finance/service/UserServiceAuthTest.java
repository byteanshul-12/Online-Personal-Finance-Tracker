package com.finance.service;

import com.finance.dao.UserDAO;
import com.finance.exception.AuthenticationException;
import com.finance.exception.ValidationException;
import com.finance.model.User;
import com.finance.testutil.DatabaseTestSupport;
import com.finance.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceAuthTest {

    private UserService userService;
    private UserDAO userDAO;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.resetSchema();
        userDAO = new UserDAO();
        userService = new UserService(userDAO);
    }

    @Test
    void registerHashesPasswordWithBcryptAndLoginVerifiesIt() throws Exception {
        User registered = userService.register("Alice", "alice@example.com", "Pass1234", "USER");

        assertNotNull(registered.getId());
        assertNotEquals("Pass1234", registered.getPassword());
        assertTrue(PasswordUtil.isHashed(registered.getPassword()));

        User stored = userDAO.findByEmail("alice@example.com");
        assertNotNull(stored);
        assertTrue(PasswordUtil.isHashed(stored.getPassword()));

        User loggedIn = userService.login("alice@example.com", "Pass1234");
        assertEquals(registered.getId(), loggedIn.getId());
        assertEquals("USER", loggedIn.getRole());
    }

    @Test
    void loginRejectsWrongPasswordAndDuplicateRegistration() throws Exception {
        userService.register("Bob", "bob@example.com", "MyPass99", "USER");

        assertThrows(AuthenticationException.class,
                () -> userService.login("bob@example.com", "Wrong999"));
        assertThrows(AuthenticationException.class,
                () -> userService.login("nobody@example.com", "MyPass99"));
        assertThrows(ValidationException.class,
                () -> userService.register("Bob2", "bob@example.com", "MyPass99", "USER"));
    }

    @Test
    void loginUpgradesLegacyPlaintextPasswordToBcrypt() throws Exception {
        User legacy = new User("USR_LEGACY1", "Legacy User", "USER", "oldplain", "legacy@example.com");
        assertTrue(userDAO.createUser(legacy));

        User loggedIn = userService.login("legacy@example.com", "oldplain");
        assertTrue(PasswordUtil.isHashed(loggedIn.getPassword()));

        User reloaded = userDAO.findById("USR_LEGACY1");
        assertNotNull(reloaded);
        assertTrue(PasswordUtil.isHashed(reloaded.getPassword()));
        assertTrue(PasswordUtil.verify("oldplain", reloaded.getPassword()));
    }

    @Test
    void changePasswordVerifiesOldPasswordAndStoresNewBcryptHash() throws Exception {
        User user = userService.register("Carol", "carol@example.com", "Init1234", "USER");

        assertThrows(AuthenticationException.class,
                () -> userService.changePassword(user.getId(), "WrongOld", "NewPass99"));

        userService.changePassword(user.getId(), "Init1234", "NewPass99");

        assertThrows(AuthenticationException.class,
                () -> userService.login("carol@example.com", "Init1234"));
        User reauthed = userService.login("carol@example.com", "NewPass99");
        assertEquals(user.getId(), reauthed.getId());
    }
}
