package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.VaccineRuleConstants;
import com.example.babyoi_be.domain.dto.request.ProfileRequest;
import com.example.babyoi_be.domain.dto.respone.ProfileResponse;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.domain.entity.VaccineRecord;
import com.example.babyoi_be.repository.ChildDiseaseDoseScheduleRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.repository.VaccineRecordRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.ProfileAvatarStorageService;
import com.example.babyoi_be.service.ProfileService;
import com.example.babyoi_be.service.TypeValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {
    private static final int CHILD_STANDARD_SCHEDULE_MAX_MONTHS = 24;

    private final ProfileRepository profileRepository;
    private final UsersRepository usersRepository;
    private final ChildDiseaseDoseScheduleRepository childDiseaseDoseScheduleRepository;
    private final VaccineRecordRepository vaccineRecordRepository;
    private final ProfileAvatarStorageService profileAvatarStorageService;
    private final TypeValueService typeValueService;

    @Override
    @Transactional
    public ProfileResponse createProfile(ProfileRequest request) {
        validateProfileRequest(request);
        validateCurrentUser(request.getUserId());

        Users user = usersRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String profileType = normalizeCode(request.getProfileType());
        validateProfileLimit(user.getId(), profileType, null);

        Profile profile = new Profile();
        BeanUtils.copyProperties(request, profile);
        LocalDateTime now = LocalDateTime.now();
        String actorName = resolveAuditName(user);

        profile.setUser(user);
        profile.setProfileType(profileType);
        profile.setSex(Profile.Sex.valueOf(resolveSex(profileType, request.getSex())));
        profile.setImageUrl(normalizeImageUrl(request));
        profile.setCreatedAt(now);
        profile.setCreatedBy(actorName);
        profile.setUpdatedAt(now);
        profile.setUpdatedBy(actorName);
        profile.setStatus(Constants.TABLE_STATUS.ACTIVE);

        Profile savedProfile = profileRepository.save(profile);
        syncStandardChildVaccineRecords(savedProfile, user.getId());
        return mapToResponse(savedProfile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long id, ProfileRequest request) {
        validateProfileRequest(request);
        validateCurrentUser(request.getUserId());

        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        validateActiveProfile(profile);
        validateRequestUserOwnsProfile(request, profile);
        validateCurrentUser(profile.getUser().getId());
        Users updater = usersRepository.findById(request.getUserId())
                .orElse(profile.getUser());
        String previousProfileType = profile.getProfileType();
        LocalDate previousDateOfBirth = profile.getDateOfBirth();

        String profileType = normalizeCode(request.getProfileType());
        validateProfileLimit(profile.getUser().getId(), profileType, profile.getId());

        BeanUtils.copyProperties(request, profile, "id", "userId", "sex");

        profile.setProfileType(profileType);
        profile.setSex(Profile.Sex.valueOf(resolveSex(profileType, request.getSex())));
        String oldImageUrl = profile.getImageUrl();
        String newImageUrl = normalizeImageUrl(request);
        profile.setImageUrl(newImageUrl);
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setUpdatedBy(resolveAuditName(updater));

        Profile savedProfile = profileRepository.save(profile);
        if ("CHILD".equals(savedProfile.getProfileType())
                && (!"CHILD".equals(previousProfileType) || !savedProfile.getDateOfBirth().equals(previousDateOfBirth))) {
            syncStandardChildVaccineRecords(savedProfile, updater.getId());
        }
        deleteOldAvatarIfChanged(oldImageUrl, newImageUrl);
        return mapToResponse(savedProfile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfileAvatar(Long id, String imageUrl) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        validateActiveProfile(profile);
        validateCurrentUser(profile.getUser().getId());

        String normalizedImageUrl = normalizeRemoteImageUrl(imageUrl);
        if (normalizedImageUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar URL không hợp lệ");
        }

        String oldImageUrl = profile.getImageUrl();
        profile.setImageUrl(normalizedImageUrl);
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setUpdatedBy(resolveAuditName(profile.getUser()));

        Profile savedProfile = profileRepository.save(profile);
        deleteOldAvatarIfChanged(oldImageUrl, normalizedImageUrl);
        log.info("Profile {} avatar updated", savedProfile.getId());
        return mapToResponse(savedProfile);
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        validateActiveProfile(profile);
        validateCurrentUser(profile.getUser().getId());

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
        String name = request.getName() != null ? request.getName().trim() : "";
        String profileType = normalizeCode(request.getProfileType());
        String sex = resolveSex(profileType, request.getSex());

        if (name.length() < 2 || name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên hồ sơ cần từ 2 đến 50 ký tự");
        }
        if (!typeValueService.existsValueCode("PROFILE_TYPE", profileType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại hồ sơ chỉ được là MOTHER hoặc CHILD");
        }
        if (!typeValueService.existsValueCode("SEX", sex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới tính chỉ được là MALE, FEMALE hoặc OTHER");
        }
        if ("MOTHER".equals(profileType) && !"FEMALE".equals(sex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ mẹ mặc định là FEMALE");
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

    private void validateProfileLimit(Long userId, String profileType, Long excludedProfileId) {
        long currentActiveCount = excludedProfileId == null
                ? profileRepository.countByUserIdAndProfileTypeAndStatus(userId, profileType, Constants.TABLE_STATUS.ACTIVE)
                : profileRepository.countByUserIdAndProfileTypeAndStatusAndIdNot(userId, profileType, Constants.TABLE_STATUS.ACTIVE, excludedProfileId);
        long limit = "MOTHER".equals(profileType) ? 1 : 2;

        if (currentActiveCount >= limit) {
            String message = "MOTHER".equals(profileType)
                    ? "Mỗi tài khoản chỉ được tạo tối đa 1 hồ sơ mẹ đang hoạt động"
                    : "Mỗi tài khoản chỉ được tạo tối đa 2 hồ sơ bé đang hoạt động";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void syncStandardChildVaccineRecords(Profile profile, Long actorId) {
        if (profile == null || !"CHILD".equals(profile.getProfileType()) || profile.getDateOfBirth() == null) {
            return;
        }

        Map<String, VaccineRecord> existingStandardRecords = new HashMap<>();
        vaccineRecordRepository.findByProfileIdAndSource(profile.getId(), VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                .stream()
                .filter(record -> record.getDisease() != null && record.getDisease().getId() != null)
                .filter(record -> record.getDoseOrder() != null)
                .forEach(record -> existingStandardRecords.putIfAbsent(recordKey(record.getDisease().getId(), record.getDoseOrder()), record));

        List<VaccineRecord> recordsToSave = new ArrayList<>();
        childDiseaseDoseScheduleRepository
                .findByStatusOrderByDiseaseDisplayOrderAscDoseOrderAsc(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .filter(schedule -> schedule.getDisease() != null)
                .filter(schedule -> schedule.getRecommendedAgeMonths() != null)
                .filter(schedule -> schedule.getRecommendedAgeMonths() >= 0)
                .filter(schedule -> schedule.getRecommendedAgeMonths() <= CHILD_STANDARD_SCHEDULE_MAX_MONTHS)
                .forEach(schedule -> {
                    LocalDate expectedInjectionDate = profile.getDateOfBirth().plusMonths(schedule.getRecommendedAgeMonths());
                    VaccineRecord existingRecord = existingStandardRecords.get(recordKey(schedule.getDisease().getId(), schedule.getDoseOrder()));
                    if (existingRecord == null) {
                        recordsToSave.add(VaccineRecord.builder()
                                .profileId(profile.getId())
                                .disease(schedule.getDisease())
                                .doseOrder(schedule.getDoseOrder())
                                .source(VaccineRuleConstants.RECORD_SOURCE.STANDARD)
                                .injectionDate(expectedInjectionDate)
                                .status(Constants.TABLE_STATUS.PENDING)
                                .createdAt(LocalDate.now())
                                .createdBy(actorId)
                                .build());
                        return;
                    }

                    if (Constants.TABLE_STATUS.PENDING.equals(existingRecord.getStatus())
                            && !expectedInjectionDate.equals(existingRecord.getInjectionDate())) {
                        existingRecord.setInjectionDate(expectedInjectionDate);
                        existingRecord.setUpdatedAt(LocalDate.now());
                        existingRecord.setUpdatedBy(actorId);
                        recordsToSave.add(existingRecord);
                    }
                });

        if (!recordsToSave.isEmpty()) {
            vaccineRecordRepository.saveAll(recordsToSave);
        }
    }

    private String recordKey(Long diseaseId, Integer doseOrder) {
        return diseaseId + ":" + doseOrder;
    }

    private String resolveSex(String profileType, String sex) {
        if ("MOTHER".equals(profileType) && (sex == null || sex.isBlank())) {
            return "FEMALE";
        }
        return normalizeCode(sex);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void validateActiveProfile(Profile profile) {
        if (!Constants.TABLE_STATUS.ACTIVE.equals(profile.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile is not active");
        }
    }

    private void validateRequestUserOwnsProfile(ProfileRequest request, Profile profile) {
        if (profile.getUser() == null || !profile.getUser().getId().equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID does not match profile owner");
        }
    }

    private String normalizeImageUrl(ProfileRequest request) {
        String normalizedImageUrl = normalizeRemoteImageUrl(request.getImageUrl());
        if (normalizedImageUrl != null) {
            return normalizedImageUrl;
        }
        return "MOTHER".equals(normalizeCode(request.getProfileType()))
                ? "https://cdn-icons-png.flaticon.com/512/4140/4140047.png"
                : "https://cdn-icons-png.flaticon.com/512/4140/4140048.png";
    }

    private String normalizeRemoteImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        String normalizedImageUrl = imageUrl.trim();
        if (normalizedImageUrl.startsWith("http://") || normalizedImageUrl.startsWith("https://")) {
            return normalizedImageUrl;
        }

        return null;
    }

    private void deleteOldAvatarIfChanged(String oldImageUrl, String newImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            return;
        }
        if (newImageUrl != null && oldImageUrl.trim().equals(newImageUrl.trim())) {
            return;
        }
        profileAvatarStorageService.deleteAvatar(oldImageUrl);
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
            return;
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
