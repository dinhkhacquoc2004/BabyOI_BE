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
import com.example.babyoi_be.security.AuthCookieService;
import com.example.babyoi_be.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class   AuthController {
    private static final String CLIENT_PLATFORM_HEADER = "X-Client-Platform";
    private static final String MOBILE_PLATFORM = "mobile";

    private final AuthService authService;
    private final AuthCookieService authCookieService;

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
    public AuthResponse verifyRegistration(
            @Valid @RequestBody VerifyOtpRequest request,
            @RequestHeader(name = CLIENT_PLATFORM_HEADER, defaultValue = "web") String clientPlatform,
            HttpServletResponse response
    ) {
        return secureAuthResponse(authService.verifyRegistration(request), clientPlatform, response);
    }

    @PostMapping("/register/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRegistration(@Valid @RequestBody OtpRequest request) {
        authService.cancelRegistration(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(name = CLIENT_PLATFORM_HEADER, defaultValue = "web") String clientPlatform,
            HttpServletResponse response
    ) {
        return secureAuthResponse(authService.login(request), clientPlatform, response);
    }

    @PostMapping("/token")
    public AuthResponse issueToken(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(name = CLIENT_PLATFORM_HEADER, defaultValue = "web") String clientPlatform,
            HttpServletResponse response
    ) {
        return secureAuthResponse(authService.issueToken(request), clientPlatform, response);
    }

    @PostMapping("/refresh-token")
    public AuthResponse refreshToken(
            @CookieValue(name = AuthCookieService.REFRESH_TOKEN_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestHeader(name = CLIENT_PLATFORM_HEADER, defaultValue = "web") String clientPlatform,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(cookieToken, request, clientPlatform);
        AuthResponse authResponse = authService.refreshToken(
                RefreshTokenRequest.builder().refreshToken(refreshToken).build()
        );
        return secureAuthResponse(authResponse, clientPlatform, response);
    }

    @PostMapping("/social-login")
    public AuthResponse socialLogin(
            @Valid @RequestBody SocialLoginRequest request,
            @RequestHeader(name = CLIENT_PLATFORM_HEADER, defaultValue = "web") String clientPlatform,
            HttpServletResponse response
    ) {
        return secureAuthResponse(authService.socialLogin(request), clientPlatform, response);
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
            @CookieValue(name = AuthCookieService.REFRESH_TOKEN_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest request,
            @RequestHeader(name = CLIENT_PLATFORM_HEADER, defaultValue = "web") String clientPlatform,
            HttpServletResponse response
    ) {
        String refreshToken = resolveOptionalRefreshToken(cookieToken, request, clientPlatform);
        RefreshTokenRequest logoutRequest = refreshToken == null
                ? null
                : RefreshTokenRequest.builder().refreshToken(refreshToken).build();

        authService.logout(logoutRequest);
        authCookieService.clearRefreshToken(response);
    }

    private AuthResponse secureAuthResponse(
            AuthResponse authResponse,
            String clientPlatform,
            HttpServletResponse response
    ) {
        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("Authentication response did not contain a refresh token");
        }

        if (MOBILE_PLATFORM.equalsIgnoreCase(clientPlatform.trim())) {
            return authResponse;
        }

        authCookieService.writeRefreshToken(response, refreshToken);
        authResponse.setRefreshToken(null);
        return authResponse;
    }

    private String resolveRefreshToken(
            String cookieToken,
            RefreshTokenRequest request,
            String clientPlatform
    ) {
        String refreshToken = resolveOptionalRefreshToken(cookieToken, request, clientPlatform);
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }
        return refreshToken;
    }

    private String resolveOptionalRefreshToken(
            String cookieToken,
            RefreshTokenRequest request,
            String clientPlatform
    ) {
        String token = MOBILE_PLATFORM.equalsIgnoreCase(clientPlatform.trim())
                ? request != null ? request.getRefreshToken() : null
                : cookieToken;
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        return null;
    }
}
