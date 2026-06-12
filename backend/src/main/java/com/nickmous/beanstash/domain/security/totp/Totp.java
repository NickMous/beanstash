package com.nickmous.beanstash.domain.security.totp;

import com.google.common.io.BaseEncoding;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Totp {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int SECRET_LENGTH_BYTES = 20;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int[] POWER_OF_TEN = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000};
    private Totp() {
    }

    public static String generateCode(byte[] secret, Instant time) {
        long counter = time.getEpochSecond() / TIME_STEP_SECONDS;
        return generateCodeForCounter(secret, counter);
    }

    public static boolean verifyCode(byte[] secret, String code, Instant time, int windowSize) {
        if (secret == null || secret.length == 0) {
            return false;
        }
        if (code == null || code.isEmpty()) {
            return false;
        }
        long currentCounter = time.getEpochSecond() / TIME_STEP_SECONDS;
        for (long i = -windowSize; i <= windowSize; i++) {
            String candidate = generateCodeForCounter(secret, currentCounter + i);
            if (MessageDigest.isEqual(
                    candidate.getBytes(StandardCharsets.UTF_8),
                    code.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    public static byte[] generateSecret() {
        byte[] secret = new byte[SECRET_LENGTH_BYTES];
        new SecureRandom().nextBytes(secret);
        return secret;
    }

    public static String secretToBase32(byte[] data) {
        return BaseEncoding.base32().encode(data);
    }

    public static String buildOtpAuthUri(String issuer, String account, byte[] secret) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8);
        String base32Secret = secretToBase32(secret);

        return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount
            + "?secret=" + base32Secret
            + "&issuer=" + encodedIssuer
            + "&algorithm=SHA1"
            + "&digits=" + CODE_DIGITS
            + "&period=" + TIME_STEP_SECONDS;
    }

    private static String generateCodeForCounter(byte[] secret, long counter) {
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

            int otp = binary % POWER_OF_TEN[CODE_DIGITS];
            String otpFormat = "%0" + CODE_DIGITS + "d";
            return otpFormat.formatted(otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to generate TOTP code", e);
        }
    }
}
