package com.finance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashGeneratesSaltedBcryptDigest() {
        String h1 = PasswordUtil.hash("Secret99");
        String h2 = PasswordUtil.hash("Secret99");

        assertNotNull(h1);
        assertEquals(60, h1.length());
        assertTrue(PasswordUtil.isHashed(h1));
        assertNotEquals(h1, h2, "Each BCrypt hash should use a unique random salt");
        assertTrue(PasswordUtil.verify("Secret99", h1));
        assertFalse(PasswordUtil.verify("WrongPass", h1));
    }

    @Test
    void verifySupportsLegacyPlaintextFallback() {
        assertFalse(PasswordUtil.isHashed("legacy123"));
        assertTrue(PasswordUtil.verify("legacy123", "legacy123"));
        assertFalse(PasswordUtil.verify("wrong123", "legacy123"));
        assertFalse(PasswordUtil.verify(null, "legacy123"));
    }

    @Test
    void hashRejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
    }
}
