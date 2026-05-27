package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.AuthResponse;
import com.example.babyoi_be.domain.dto.LoginRequest;
import com.example.babyoi_be.domain.dto.OtpRequest;
import com.example.babyoi_be.domain.dto.OtpResponse;
import com.example.babyoi_be.domain.dto.RefreshTokenRequest;
import com.example.babyoi_be.domain.dto.RegisterRequest;
import com.example.babyoi_be.domain.dto.ResetPasswordRequest;
import com.example.babyoi_be.domain.dto.SocialLoginRequest;
import com.example.babyoi_be.domain.dto.VerifyOtpRequest;
import com.example.babyoi_be.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class   AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public OtpResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/resend-otp")
    public OtpResponse resendRegistrationOtp(@Valid @RequestBody OtpRequest request) {
        return authService.resendRegistrationOtp(request);
    }

    @PostMapping("/register/verify")
    public AuthResponse verifyRegistration(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyRegistration(request);
    }

    @PostMapping("/register/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRegistration(@Valid @RequestBody OtpRequest request) {
        authService.cancelRegistration(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/token")
    public AuthResponse issueToken(@Valid @RequestBody LoginRequest request) {
        return authService.issueToken(request);
    }

    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @PostMapping("/social-login")
    public AuthResponse socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        return authService.socialLogin(request);
    }

    @PostMapping("/forgot-password")
    public OtpResponse forgotPassword(@Valid @RequestBody OtpRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/forgot-password/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyPasswordResetCode(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyPasswordResetCode(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody(required = false) RefreshTokenRequest request
    ) {
        authService.logout(authorizationHeader, request);
    }
}
