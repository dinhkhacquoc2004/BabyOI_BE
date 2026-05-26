package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.ProfileRequest;
import com.example.babyoi_be.domain.dto.request.ProfileAvatarUpdateRequest;
import com.example.babyoi_be.domain.dto.respone.ProfileAvatarUploadResponse;
import com.example.babyoi_be.domain.dto.respone.ProfileResponse;
import com.example.babyoi_be.service.ProfileAvatarStorageService;
import com.example.babyoi_be.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileAvatarStorageService profileAvatarStorageService;

    public ProfileController(ProfileService profileService, ProfileAvatarStorageService profileAvatarStorageService) {
        this.profileService = profileService;
        this.profileAvatarStorageService = profileAvatarStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse createProfile(@Valid @RequestBody ProfileRequest request) {
        return profileService.createProfile(request);
    }

    @PutMapping("/{id}")
    public ProfileResponse updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileRequest request) {
        return profileService.updateProfile(id, request);
    }

    @PostMapping("/avatar")
    public ProfileAvatarUploadResponse uploadProfileAvatar(@RequestParam("file") MultipartFile file) {
        return new ProfileAvatarUploadResponse(profileAvatarStorageService.uploadAvatar(file));
    }

    @PatchMapping("/{id}/avatar")
    public ProfileResponse updateProfileAvatar(@PathVariable Long id, @Valid @RequestBody ProfileAvatarUpdateRequest request) {
        return profileService.updateProfileAvatar(id, request.getImageUrl());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable Long id) {
        profileService.deleteProfile(id);
    }

    @GetMapping("/{id}")
    public ProfileResponse getProfileById(@PathVariable Long id) {
        return profileService.getProfileById(id);
    }

    @GetMapping("/user/{userId}")
    public List<ProfileResponse> getProfilesByUserId(@PathVariable Long userId) {
        return profileService.getProfilesByUserId(userId);
    }
}
