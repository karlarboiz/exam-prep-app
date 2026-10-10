package com.examprep.util;

import com.examprep.config.AppConfig;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Opaque URL-safe encryption for entity ids. AES/GCM with a random IV so
 * tokens differ each render. {@link #dec(String)} also accepts plain numeric
 * strings so POST hidden fields can stay unchanged.
 */
public final class IdCipher {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdCipher() {
    }

    public static String enc(long id) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plaintext = ByteBuffer.allocate(Long.BYTES).putLong(id).array();
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt id", e);
        }
    }

    /**
     * Decrypts an opaque token, or parses a plain numeric string (for POST bodies).
     */
    public static long dec(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing id token");
        }
        String trimmed = token.trim();
        if (isPlainNumeric(trimmed)) {
            return Long.parseLong(trimmed);
        }
        try {
            byte[] combined = Base64.getUrlDecoder().decode(trimmed);
            if (combined.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid id token");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return ByteBuffer.wrap(plaintext).getLong();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid id token", e);
        }
    }

    private static boolean isPlainNumeric(String value) {
        if (value.isEmpty()) {
            return false;
        }
        int i = 0;
        if (value.charAt(0) == '-') {
            if (value.length() == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static SecretKey getKey() {
        try {
            String secret = AppConfig.get("id.cipher.secret", "change-me-id-cipher-secret");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive id cipher key", e);
        }
    }
}
