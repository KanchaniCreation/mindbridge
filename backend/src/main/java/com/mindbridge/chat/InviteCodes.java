package com.mindbridge.chat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/** Generates, formats and hashes invite codes. Characters that look alike (0/O, 1/I) are left out. */
public final class InviteCodes {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCodes() {
    }

    public static String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** XXXXX-XXXXX, easier to read and type. */
    public static String format(String code) {
        return code.substring(0, 5) + "-" + code.substring(5);
    }

    /** Accepts lower case, spaces and dashes, so typing "abcde-fghjk" works. */
    public static String normalize(String input) {
        return input.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    public static String hash(String normalizedCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedCode.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
