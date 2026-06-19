package com.example.babyoi_be.exception;

public class EmailVerificationRequiredException extends RuntimeException {
    public static final String ERROR_CODE = "EMAIL_VERIFICATION_REQUIRED";
    public static final String ACTION = "VERIFY_REGISTRATION";

    private final String email;
    private final int expiresInMinutes;

    public EmailVerificationRequiredException(String email, int expiresInMinutes) {
        super("auth.login.email-verification-required");
        this.email = email;
        this.expiresInMinutes = expiresInMinutes;
    }

    public String getEmail() {
        return email;
    }

    public int getExpiresInMinutes() {
        return expiresInMinutes;
    }
}
