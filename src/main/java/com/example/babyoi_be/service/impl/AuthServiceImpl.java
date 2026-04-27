package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.AuthResponse;
import com.example.babyoi_be.domain.dto.LoginRequest;
import com.example.babyoi_be.domain.dto.RegisterRequest;
import com.example.babyoi_be.domain.dto.UserProfileResponse;
import com.example.babyoi_be.domain.entity.Roles;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.RolesRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.security.SessionTokenService;
import com.example.babyoi_be.service.AuthService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE_NAME = "MOTHER";

    private final UsersRepository usersRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SessionTokenService sessionTokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
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
                .roles(defaultRole)
                .build();

        Users savedUser = usersRepository.save(user);
        String accessToken = sessionTokenService.issueToken(savedUser);

        return buildAuthResponse(savedUser, accessToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.login.invalid-credentials"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.login.invalid-credentials");
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.login.account-disabled");
        }

        return buildAuthResponse(user, sessionTokenService.issueToken(user));
    }

    @Override
    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "auth.authorization.invalid");
        }

        String rawToken = authorizationHeader.substring(7);
        sessionTokenService.revoke(rawToken);
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

    private AuthResponse buildAuthResponse(Users user, String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRoles() != null ? user.getRoles().getName() : null)
                .build();
    }
}
