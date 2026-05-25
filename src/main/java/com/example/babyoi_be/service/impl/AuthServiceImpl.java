package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
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
import com.example.babyoi_be.domain.entity.AuthOtp;
import com.example.babyoi_be.domain.entity.AuthRefreshToken;
import com.example.babyoi_be.domain.entity.Roles;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.AuthOtpRepository;
import com.example.babyoi_be.repository.AuthRefreshTokenRepository;
import com.example.babyoi_be.repository.RolesRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.security.JwtService;
import com.example.babyoi_be.service.AuthService;
import com.example.babyoi_be.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE_NAME = "MOTHER";
    private static final int OTP_EXPIRES_IN_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsersRepository usersRepository;
    private final RolesRepository rolesRepository;
    private final AuthOtpRepository authOtpRepository;
    private final AuthRefreshTokenRepository authRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailNotificationService emailNotificationService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;

    @Value("${app.jwt.refresh-expiration-millis:2592000000}")
    private long refreshTokenExpirationMillis;

    @Override
    @Transactional
    public OtpResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedUserName = request.getUserName().trim();

        if (usersRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "auth.register.email-exists");
        }

        if (usersRepository.existsByUserName(normalizedUserName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "auth.register.username-exists");
        }

        Roles defaultRole = rolesRepository.findByNameIgnoreCase(DEFAULT_ROLE_NAME)
                .orElseGet(this::createDefaultRole);

        Users user = Users.builder()
                .userName(normalizedUserName)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(Constants.TABLE_STATUS.ACTIVE)
                .emailVerified(false)
                .roles(defaultRole)
                .build();

        usersRepository.save(user);
        createAndSendOtp(normalizedEmail, AuthOtp.Purpose.REGISTER_VERIFY);

        return buildOtpResponse(normalizedEmail, "Mã xác nhận đã được gửi đến email của bạn");
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.login.invalid-credentials"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            createAndSendOtp(normalizedEmail, AuthOtp.Purpose.REGISTER_VERIFY);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vui lòng xác nhận email trước khi đăng nhập");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.login.invalid-credentials");
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.login.account-disabled");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse issueToken(LoginRequest request) {
        return login(request);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken().trim();
        AuthRefreshToken storedToken = authRefreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(hashToken(refreshToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token khong hop le"));

        LocalDateTime now = LocalDateTime.now();
        if (storedToken.getExpiresAt().isBefore(now)) {
            storedToken.setRevokedAt(now);
            authRefreshTokenRepository.save(storedToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token da het han");
        }

        storedToken.setLastUsedAt(now);
        storedToken.setRevokedAt(now);
        authRefreshTokenRepository.save(storedToken);

        Users user = storedToken.getUser();
        if (!Constants.TABLE_STATUS.ACTIVE.equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tai khoan da bi khoa");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        SocialAccount account = resolveSocialAccount(request);

        Users user = usersRepository.findByEmail(account.email())
                .map(existingUser -> updateSocialUser(existingUser, account))
                .orElseGet(() -> createSocialUser(account));

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public OtpResponse resendRegistrationOtp(OtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return buildOtpResponse(normalizedEmail, "Email đã được xác nhận");
        }

        createAndSendOtp(normalizedEmail, AuthOtp.Purpose.REGISTER_VERIFY);
        return buildOtpResponse(normalizedEmail, "Mã xác nhận đã được gửi đến email của bạn");
    }

    @Override
    @Transactional
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateOtp(normalizedEmail, request.getCode(), AuthOtp.Purpose.REGISTER_VERIFY, true);

        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
        user.setEmailVerified(true);
        user.setStatus(Constants.TABLE_STATUS.ACTIVE);
        usersRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void cancelRegistration(OtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản đang tạo"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác nhận, không thể hủy tạo tài khoản");
        }

        authOtpRepository.deleteByEmail(normalizedEmail);
        usersRepository.delete(user);
    }

    @Override
    @Transactional
    public OtpResponse forgotPassword(OtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản chưa được xác nhận email");
        }

        createAndSendOtp(normalizedEmail, AuthOtp.Purpose.PASSWORD_RESET);

        return buildOtpResponse(normalizedEmail, "Mã xác nhận đã được gửi đến email của bạn");
    }

    @Override
    @Transactional
    public void verifyPasswordResetCode(VerifyOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateOtp(normalizedEmail, request.getCode(), AuthOtp.Purpose.PASSWORD_RESET, false);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateOtp(normalizedEmail, request.getCode(), AuthOtp.Purpose.PASSWORD_RESET, false);

        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        validateOtp(normalizedEmail, request.getCode(), AuthOtp.Purpose.PASSWORD_RESET, true);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        usersRepository.save(user);
    }

    @Override
    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "auth.authorization.invalid");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.unauthorized");
        }

        Users user = usersRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.unauthorized"));

        return UserProfileResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRoles() != null ? user.getRoles().getName() : null)
                .build();
    }

    private Roles createDefaultRole() {
        Roles role = Roles.builder()
                .name(DEFAULT_ROLE_NAME)
                .status(Constants.TABLE_STATUS.ACTIVE)
                .build();

        return rolesRepository.save(role);
    }

    private SocialAccount resolveSocialAccount(SocialLoginRequest request) {
        String provider = request.getProvider() != null
                ? request.getProvider().trim().toUpperCase(Locale.ROOT)
                : "";

        return switch (provider) {
            case "GOOGLE" -> resolveGoogleAccount(request.getIdToken());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhà cung cấp đăng nhập không hợp lệ");
        };
    }

    private SocialAccount resolveGoogleAccount(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google token không hợp lệ");
        }

        try {
            String response = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("oauth2.googleapis.com")
                            .path("/tokeninfo")
                            .queryParam("id_token", idToken)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode payload = objectMapper.readTree(response);
            String audience = text(payload, "aud");

            if (!isAllowedGoogleAudience(audience)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token không đúng ứng dụng");
            }

            String email = normalizeEmail(text(payload, "email"));
            if (email.isBlank() || !"true".equalsIgnoreCase(text(payload, "email_verified"))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email chưa được xác nhận");
            }

            return new SocialAccount(
                    "GOOGLE",
                    text(payload, "sub"),
                    email,
                    firstNonBlank(text(payload, "name"), email.substring(0, email.indexOf("@")))
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác thực được tài khoản Google");
        }
    }

    private Users createSocialUser(SocialAccount account) {
        Roles defaultRole = rolesRepository.findByNameIgnoreCase(DEFAULT_ROLE_NAME)
                .orElseGet(this::createDefaultRole);

        Users user = Users.builder()
                .userName(generateUniqueUserName(account.name(), account.email()))
                .email(account.email())
                .passwordHash(passwordEncoder.encode("SOCIAL_LOGIN_" + UUID.randomUUID()))
                .status(Constants.TABLE_STATUS.ACTIVE)
                .emailVerified(true)
                .socialProvider(account.provider())
                .socialProviderId(account.providerUserId())
                .roles(defaultRole)
                .build();

        return usersRepository.save(user);
    }

    private Users updateSocialUser(Users user, SocialAccount account) {
        user.setEmailVerified(true);
        user.setStatus(Constants.TABLE_STATUS.ACTIVE);
        user.setSocialProvider(account.provider());
        user.setSocialProviderId(account.providerUserId());
        return usersRepository.save(user);
    }

    private String generateUniqueUserName(String rawName, String email) {
        String baseName = firstNonBlank(rawName, email.substring(0, email.indexOf("@")))
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (baseName.isBlank()) {
            baseName = "babyoi_user";
        }

        String candidate = baseName;
        int index = 1;
        while (usersRepository.existsByUserName(candidate)) {
            candidate = baseName + "_" + index;
            index++;
        }
        return candidate;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node != null ? node.get(fieldName) : null;
        return value != null && !value.isNull() ? value.asText("") : "";
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private boolean isAllowedGoogleAudience(String audience) {
        if (googleClientId == null || googleClientId.isBlank()) {
            return true;
        }

        return Arrays.stream(googleClientId.split(","))
                .map(String::trim)
                .filter(clientId -> !clientId.isBlank())
                .anyMatch(clientId -> clientId.equals(audience));
    }

    private void createAndSendOtp(String email, AuthOtp.Purpose purpose) {
        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();

        AuthOtp otp = AuthOtp.builder()
                .email(email)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(now.plusMinutes(OTP_EXPIRES_IN_MINUTES))
                .attempts(0)
                .createdAt(now)
                .build();

        authOtpRepository.save(otp);
        emailNotificationService.sendOtp(email, buildOtpSubject(purpose), code, OTP_EXPIRES_IN_MINUTES);
    }

    private void validateOtp(String email, String code, AuthOtp.Purpose purpose, boolean markAsUsed) {
        AuthOtp otp = authOtpRepository.findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ hoặc đã hết hạn"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ hoặc đã hết hạn");
        }

        if (otp.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận đã nhập sai quá nhiều lần");
        }

        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            authOtpRepository.save(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận không đúng");
        }

        if (markAsUsed) {
            otp.setUsedAt(LocalDateTime.now());
            authOtpRepository.save(otp);
        }
    }

    private String buildOtpSubject(AuthOtp.Purpose purpose) {
        return switch (purpose) {
            case REGISTER_VERIFY -> "BabyOi email verification code";
            case PASSWORD_RESET -> "BabyOi password reset code";
        };
    }

    private OtpResponse buildOtpResponse(String email, String message) {
        return OtpResponse.builder()
                .email(email)
                .message(message)
                .expiresInMinutes(OTP_EXPIRES_IN_MINUTES)
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private AuthResponse buildAuthResponse(Users user) {
        IssuedRefreshToken issuedRefreshToken = issueRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(issuedRefreshToken.token())
                .tokenType("Bearer")
                .accessTokenExpiresInMillis(jwtService.getExpirationMillis())
                .refreshTokenExpiresInMillis(refreshTokenExpirationMillis)
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRoles() != null ? user.getRoles().getName() : null)
                .build();
    }

    private IssuedRefreshToken issueRefreshToken(Users user) {
        String token = generateRefreshToken();
        LocalDateTime now = LocalDateTime.now();

        AuthRefreshToken refreshToken = AuthRefreshToken.builder()
                .tokenHash(hashToken(token))
                .user(user)
                .issuedAt(now)
                .lastUsedAt(now)
                .expiresAt(now.plus(Duration.ofMillis(refreshTokenExpirationMillis)))
                .build();

        authRefreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(token);
    }

    private String generateRefreshToken() {
        byte[] tokenBytes = new byte[48];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append("%02x".formatted(value & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record SocialAccount(String provider, String providerUserId, String email, String name) {
    }

    private record IssuedRefreshToken(String token) {
    }
}
