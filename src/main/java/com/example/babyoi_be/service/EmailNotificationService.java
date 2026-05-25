package com.example.babyoi_be.service;

public interface EmailNotificationService {
    void sendOtp(String toEmail, String subject, String code, int expiresInMinutes);
}
