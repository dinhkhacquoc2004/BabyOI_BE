package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.ProfileRequest;
import com.example.babyoi_be.domain.dto.respone.ProfileResponse;
import java.util.List;

public interface ProfileService {
    ProfileResponse createProfile(ProfileRequest request);
    ProfileResponse updateProfile(Long id, ProfileRequest request);
    ProfileResponse updateProfileAvatar(Long id, String imageUrl);
    void deleteProfile(Long id);
    ProfileResponse getProfileById(Long id);
    List<ProfileResponse> getProfilesByUserId(Long userId);
    
    // New methods
    boolean hasProfile(Long userId);
}
