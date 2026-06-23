package com.example.babyoi_be.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    public static final String REFRESH_TOKEN_COOKIE = "babyoi_refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final boolean secure;
    private final String sameSite;
    private final Duration refreshTokenLifetime;

    public AuthCookieService(
            @Value("${app.auth-cookie.secure:true}") boolean secure,
            @Value("${app.auth-cookie.same-site:Lax}") String sameSite,
            @Value("${app.jwt.refresh-expiration-millis:2592000000}") long refreshExpirationMillis
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.refreshTokenLifetime = Duration.ofMillis(refreshExpirationMillis);
    }

    public void writeRefreshToken(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = baseCookie(refreshToken)
                .maxAge(refreshTokenLifetime)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshToken(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH);
    }
}
