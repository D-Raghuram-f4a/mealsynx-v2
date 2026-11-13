package com.mealsynx.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cryptographic Salted SHA-256 Password & Security-Answer Hashing Utility for MealSynx V2.
 * Format: Base64(16-byte salt) + ":" + Base64(SHA-256(salt + input))
 */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public static String hash(String plainText) {
        if (plainText == null) plainText = "";
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return hashWithSalt(plainText.trim(), salt);
    }

    public static String hashAnswer(String answer) {
        if (answer == null) answer = "";
        return hash(answer.trim().toLowerCase());
    }

    public static boolean verify(String plainText, String storedHashOrPlain) {
        if (plainText == null || storedHashOrPlain == null) return false;
        String trimmedInput = plainText.trim();
        if (storedHashOrPlain.contains(":")) {
            String[] parts = storedHashOrPlain.split(":", 2);
            try {
                byte[] salt = Base64.getDecoder().decode(parts[0]);
                String expected = hashWithSalt(trimmedInput, salt);
                return MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        storedHashOrPlain.getBytes(StandardCharsets.UTF_8)
                );
            } catch (Exception ignored) {
                return false;
            }
        }
        return storedHashOrPlain.equalsIgnoreCase(trimmedInput)
                || storedHashOrPlain.replaceAll("\\s+", "").equalsIgnoreCase(trimmedInput.replaceAll("\\s+", ""));
    }

    public static boolean verifyAnswer(String plainAnswer, String storedHashOrPlain) {
        if (plainAnswer == null || storedHashOrPlain == null) return false;
        return verify(plainAnswer.trim().toLowerCase(), storedHashOrPlain);
    }

    private static String hashWithSalt(String input, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
