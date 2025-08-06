package com.yourname.filededup.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for file hash calculations
 */
public class FileHashUtil {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Calculate SHA-256 hash of an InputStream
     * @param inputStream InputStream of the file
     * @return SHA-256 hash as hexadecimal string
     * @throws IOException if reading the stream fails
     */
    public static String calculateSHA256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculate SHA-256 hash of a byte array
     * @param data Byte array
     * @return SHA-256 hash as hexadecimal string
     */
    public static String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculate MD5 hash of an InputStream (alternative hash function)
     * @param inputStream InputStream of the file
     * @return MD5 hash as hexadecimal string
     * @throws IOException if reading the stream fails
     */
    public static String calculateMD5(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Convert byte array to hexadecimal string
     * @param bytes Byte array
     * @return Hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Validate if a string is a valid SHA-256 hash
     * @param hash Hash string to validate
     * @return true if valid SHA-256 hash format, false otherwise
     */
    public static boolean isValidSHA256(String hash) {
        if (hash == null || hash.length() != 64) {
            return false;
        }
        return hash.matches("^[a-fA-F0-9]{64}$");
    }

    /**
     * Validate if a string is a valid MD5 hash
     * @param hash Hash string to validate
     * @return true if valid MD5 hash format, false otherwise
     */
    public static boolean isValidMD5(String hash) {
        if (hash == null || hash.length() != 32) {
            return false;
        }
        return hash.matches("^[a-fA-F0-9]{32}$");
    }
}
