package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.AuthResponse;
import com.example.babyoi_be.domain.dto.LoginRequest;
import com.example.babyoi_be.domain.dto.RefreshTokenRequest;
import com.example.babyoi_be.security.AuthCookieService;
import com.example.babyoi_be.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Test
    void loginMovesRefreshTokenFromJsonResponseToHttpOnlyCookie() {
        AuthCookieService cookieService = new AuthCookieService(true, "Lax", 2_592_000_000L);
        AuthController controller = new AuthController(authService, cookieService);
        LoginRequest request = new LoginRequest();
        AuthResponse serviceResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        when(authService.login(request)).thenReturn(serviceResponse);

        AuthResponse result = controller.login(request, "web", servletResponse);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isNull();
        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
                .contains(AuthCookieService.REFRESH_TOKEN_COOKIE + "=refresh-token")
                .contains("HttpOnly");
    }

    @Test
    void loginReturnsRefreshTokenToNativeMobileClient() {
        AuthCookieService cookieService = new AuthCookieService(true, "Lax", 2_592_000_000L);
        AuthController controller = new AuthController(authService, cookieService);
        LoginRequest request = new LoginRequest();
        AuthResponse serviceResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        when(authService.login(request)).thenReturn(serviceResponse);

        AuthResponse result = controller.login(request, "mobile", servletResponse);

        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void mobileRefreshIgnoresBrowserCookieAndUsesBodyToken() {
        AuthCookieService cookieService = new AuthCookieService(true, "Lax", 2_592_000_000L);
        AuthController controller = new AuthController(authService, cookieService);
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("native-body-token")
                .build();
        AuthResponse serviceResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-native-token")
                .build();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        when(authService.refreshToken(org.mockito.ArgumentMatchers.any())).thenReturn(serviceResponse);

        controller.refreshToken("browser-cookie-token", request, "mobile", servletResponse);

        ArgumentCaptor<RefreshTokenRequest> captor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        verify(authService).refreshToken(captor.capture());
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("native-body-token");
    }

    @Test
    void webRefreshIgnoresBodyTokenAndUsesHttpOnlyCookie() {
        AuthCookieService cookieService = new AuthCookieService(true, "Lax", 2_592_000_000L);
        AuthController controller = new AuthController(authService, cookieService);
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("untrusted-body-token")
                .build();
        AuthResponse serviceResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("rotated-cookie-token")
                .build();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        when(authService.refreshToken(org.mockito.ArgumentMatchers.any())).thenReturn(serviceResponse);

        AuthResponse result = controller.refreshToken("browser-cookie-token", request, "web", servletResponse);

        ArgumentCaptor<RefreshTokenRequest> captor = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        verify(authService).refreshToken(captor.capture());
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("browser-cookie-token");
        assertThat(result.getRefreshToken()).isNull();
        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
                .contains(AuthCookieService.REFRESH_TOKEN_COOKIE + "=rotated-cookie-token");
    }
}
