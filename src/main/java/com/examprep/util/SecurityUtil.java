package com.examprep.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     * Compares two strings by comparing their SHA-256 hashes using MessageDigest.isEqual.
     * 
     * @param expected the expected value
     * @param provided the provided value to compare
     * @return true if the strings are equal, false otherwise
     */
    public static boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] expectedHash = digest.digest(expected.getBytes(StandardCharsets.UTF_8));
            byte[] providedHash = digest.digest(provided.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expectedHash, providedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
