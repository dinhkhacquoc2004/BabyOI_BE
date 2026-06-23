package com.example.babyoi_be.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    private final AuthCookieService authCookieService =
            new AuthCookieService(true, "Lax", 2_592_000_000L);

    @Test
    void writesRefreshTokenAsSecureHttpOnlyCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authCookieService.writeRefreshToken(response, "refresh-secret");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .contains(AuthCookieService.REFRESH_TOKEN_COOKIE + "=refresh-secret")
                .contains("Path=/api/auth")
                .contains("Max-Age=2592000")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    void clearsRefreshTokenUsingTheSameCookieScope() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authCookieService.clearRefreshToken(response);

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie)
                .contains(AuthCookieService.REFRESH_TOKEN_COOKIE + "=")
                .contains("Path=/api/auth")
                .contains("Max-Age=0")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }
}
