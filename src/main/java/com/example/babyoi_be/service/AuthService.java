package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.AuthResponse;
import com.example.babyoi_be.domain.dto.LoginRequest;
import com.example.babyoi_be.domain.dto.RegisterRequest;
import com.example.babyoi_be.domain.dto.UserProfileResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String authorizationHeader);

    UserProfileResponse getCurrentUser();
}
