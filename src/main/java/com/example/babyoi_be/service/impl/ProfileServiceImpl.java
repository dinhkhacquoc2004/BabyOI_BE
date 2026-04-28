package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.domain.dto.request.ProfileRequest;
import com.example.babyoi_be.domain.dto.respone.ProfileResponse;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional
    public ProfileResponse createProfile(ProfileRequest request) {
        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Profile profile = new Profile();
        BeanUtils.copyProperties(request, profile);
        
        profile.setUser(user);
        if (request.getSex() != null) {
            profile.setSex(Profile.Sex.valueOf(request.getSex()));
        }
        profile.setCreatedAt(LocalDateTime.now());
        profile.setStatus(1L);

        return mapToResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long id, ProfileRequest request) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Copy properties from request to existing profile, excluding null or special fields if necessary
        // In this case, we copy most fields
        BeanUtils.copyProperties(request, profile, "id", "userId", "sex");
        
        if (request.getSex() != null) {
            profile.setSex(Profile.Sex.valueOf(request.getSex()));
        }
        profile.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        profile.setStatus(0L);
        profileRepository.save(profile);
    }

    @Override
    public ProfileResponse getProfileById(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return mapToResponse(profile);
    }

    @Override
    public List<ProfileResponse> getProfilesByUserId(Long userId) {
        return profileRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProfileResponse mapToResponse(Profile profile) {
        ProfileResponse response = new ProfileResponse();
        BeanUtils.copyProperties(profile, response, "userId", "sex");
        
        // Handle special fields that BeanUtils can't map automatically
        if (profile.getUser() != null) {
            response.setUserId(profile.getUser().getId());
        }
        if (profile.getSex() != null) {
            response.setSex(profile.getSex().name());
        }
        
        return response;
    }
}
