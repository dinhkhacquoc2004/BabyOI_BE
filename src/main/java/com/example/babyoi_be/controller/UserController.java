package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.UserProfileResponse;
import com.example.babyoi_be.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser() {
        return authService.getCurrentUser();
    }
}
