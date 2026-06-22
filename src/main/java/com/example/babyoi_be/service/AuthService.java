package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.AuthResponse;
import com.example.babyoi_be.domain.dto.LoginRequest;
import com.example.babyoi_be.domain.dto.OtpRequest;
import com.example.babyoi_be.domain.dto.OtpResponse;
import com.example.babyoi_be.domain.dto.RefreshTokenRequest;
import com.example.babyoi_be.domain.dto.RegisterRequest;
import com.example.babyoi_be.domain.dto.ResetPasswordRequest;
import com.example.babyoi_be.domain.dto.SocialLoginRequest;
import com.example.babyoi_be.domain.dto.UserProfileResponse;
import com.example.babyoi_be.domain.dto.VerifyOtpRequest;

public interface AuthService {
    OtpResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse issueToken(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse socialLogin(SocialLoginRequest request);

    OtpResponse resendRegistrationOtp(OtpRequest request);

    AuthResponse verifyRegistration(VerifyOtpRequest request);

    void cancelRegistration(OtpRequest request);

    OtpResponse forgotPassword(OtpRequest request);

    void verifyPasswordResetCode(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(String authorizationHeader, RefreshTokenRequest request);

    UserProfileResponse getCurrentUser();

    void deleteCurrentAccount();
}
