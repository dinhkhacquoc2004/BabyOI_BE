package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.ProfileRequest;
import com.example.babyoi_be.domain.dto.respone.ProfileResponse;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final UsersRepository usersRepository;

    private static final Set<String> PROFILE_TYPES = Set.of("MOTHER", "CHILD");
    private static final Set<String> SEX_VALUES = Set.of("MALE", "FEMALE", "OTHER");

    @Override
    @Transactional
    public ProfileResponse createProfile(ProfileRequest request) {
        validateProfileRequest(request);
        validateCurrentUser(request.getUserId());

        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        long activeProfileCount = profileRepository.countByUserIdAndStatusIn(user.getId(), List.of(Constants.TABLE_STATUS.ACTIVE));
        
        if (activeProfileCount >= 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi tài khoản chỉ được tạo tối đa 3 hồ sơ đang hoạt động");
        }

        Profile profile = new Profile();
        BeanUtils.copyProperties(request, profile);
        LocalDateTime now = LocalDateTime.now();
        String actorName = resolveAuditName(user);
        
        profile.setUser(user);
        profile.setProfileType(normalizeCode(request.getProfileType()));
        profile.setSex(Profile.Sex.valueOf(normalizeCode(request.getSex())));
        profile.setImageUrl(normalizeImageUrl(request));
        profile.setCreatedAt(now);
        profile.setCreatedBy(actorName);
        profile.setStatus(Constants.TABLE_STATUS.ACTIVE);

        return mapToResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long id, ProfileRequest request) {
        validateProfileRequest(request);
        validateCurrentUser(request.getUserId());

        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        validateCurrentUser(profile.getUser().getId());
        Users updater = usersRepository.findById(request.getUserId())
                .orElse(profile.getUser());

        BeanUtils.copyProperties(request, profile, "id", "userId", "sex");
        
        profile.setProfileType(normalizeCode(request.getProfileType()));
        profile.setSex(Profile.Sex.valueOf(normalizeCode(request.getSex())));
        profile.setImageUrl(normalizeImageUrl(request));
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setUpdatedBy(resolveAuditName(updater));

        return mapToResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        validateCurrentUser(profile.getUser().getId());
        
        // Soft delete: change status to INACTIVE
        profile.setStatus(Constants.TABLE_STATUS.INACTIVE);
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setUpdatedBy(resolveAuditName(profile.getUser()));
        profileRepository.save(profile);
    }

    @Override
    public ProfileResponse getProfileById(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        validateCurrentUser(profile.getUser().getId());
        return mapToResponse(profile);
    }

    @Override
    public List<ProfileResponse> getProfilesByUserId(Long userId) {
        validateCurrentUser(userId);
        return profileRepository.findByUserIdAndStatus(userId, Constants.TABLE_STATUS.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasProfile(Long userId) {
        validateCurrentUser(userId);
        return profileRepository.existsByUserIdAndStatusIn(userId, List.of(Constants.TABLE_STATUS.ACTIVE));
    }

    private void validateProfileRequest(ProfileRequest request) {
        String profileType = normalizeCode(request.getProfileType());
        String sex = normalizeCode(request.getSex());

        if (!PROFILE_TYPES.contains(profileType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại hồ sơ chỉ được là MOTHER hoặc CHILD");
        }
        if (!SEX_VALUES.contains(sex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới tính chỉ được là MALE, FEMALE hoặc OTHER");
        }
        if (request.getDateOfBirth() == null || request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày sinh không hợp lệ");
        }

        if ("CHILD".equals(profileType) && request.getDateOfBirth().isBefore(LocalDate.now().minusMonths(24))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ bé chỉ dành cho bé từ 0 đến 24 tháng");
        }
        if ("MOTHER".equals(profileType) && request.getDateOfBirth().isAfter(LocalDate.now().minusYears(13))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ mẹ cần có tuổi phù hợp");
        }
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeImageUrl(ProfileRequest request) {
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            return request.getImageUrl().trim();
        }
        return "MOTHER".equals(normalizeCode(request.getProfileType()))
                ? "https://cdn-icons-png.flaticon.com/512/4140/4140047.png"
                : "https://cdn-icons-png.flaticon.com/512/4140/4140048.png";
    }

    private String resolveAuditName(Users user) {
        if (user == null) {
            return "SYSTEM";
        }
        if (user.getUserName() != null && !user.getUserName().isBlank()) {
            return user.getUserName().trim();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail().trim();
        }
        return "USER_" + user.getId();
    }

    private void validateCurrentUser(Long userId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "auth.unauthorized");
        }
        if (!userDetails.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
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
