package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UsersRepository usersRepository;

    private static final List<Long> VALID_STATUSES = Arrays.asList(
            Constants.TABLE_STATUS.ACTIVE,
            Constants.TABLE_STATUS.INACTIVE,
            Constants.TABLE_STATUS.DELETED
    );

    @Override
    @Transactional
    public ProfileResponse createProfile(ProfileRequest request) {
        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check limit: max 2 profiles with valid status
        long activeProfileCount = profileRepository.countByUserIdAndStatusIn(user.getId(), VALID_STATUSES);
        
        if (activeProfileCount >= 2) {
            throw new RuntimeException("User already has maximum number of profiles (2)");
        }

        Profile profile = new Profile();
        BeanUtils.copyProperties(request, profile);
        
        profile.setUser(user);
        if (request.getSex() != null) {
            profile.setSex(Profile.Sex.valueOf(request.getSex()));
        }
        profile.setCreatedAt(LocalDateTime.now());
        profile.setStatus(Constants.TABLE_STATUS.INITIATED);

        return mapToResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long id, ProfileRequest request) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

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
        
        // Soft delete: change status to INACTIVE
        profile.setStatus(Constants.TABLE_STATUS.INACTIVE);
        profile.setUpdatedAt(LocalDateTime.now());
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

    @Override
    public boolean hasProfile(Long userId) {
        return profileRepository.existsByUserIdAndStatusIn(userId, VALID_STATUSES);
    }

    private ProfileResponse mapToResponse(Profile profile) {
        ProfileResponse response = new ProfileResponse();
        BeanUtils.copyProperties(profile, response, "userId", "sex");
        
        if (profile.getUser() != null) {
            response.setUserId(profile.getUser().getId());
        }
        if (profile.getSex() != null) {
            response.setSex(profile.getSex().name());
        }
        
        return response;
    }
}
