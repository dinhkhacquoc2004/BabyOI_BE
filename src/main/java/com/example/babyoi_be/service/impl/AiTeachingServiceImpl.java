package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.respone.AiTeachingLessonResponse;
import com.example.babyoi_be.domain.entity.AiLessonProgress;
import com.example.babyoi_be.domain.entity.AiTeachingLesson;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.repository.AiLessonProgressRepository;
import com.example.babyoi_be.repository.AiTeachingLessonRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.AiTeachingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiTeachingServiceImpl implements AiTeachingService {

    private final AiTeachingLessonRepository lessonRepository;
    private final AiLessonProgressRepository progressRepository;
    private final ProfileRepository profileRepository;

    @Override
    public List<AiTeachingLessonResponse> getLessonsByProfile(Long profileId) {
        validateCurrentProfile(profileId);

        List<AiTeachingLesson> lessons = lessonRepository.findByStatusOrderBySortOrderAscIdAsc(Constants.TABLE_STATUS.ACTIVE);
        Map<Long, AiLessonProgress> progressByLessonId = progressRepository.findByProfileIdAndStatus(profileId, Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .filter(progress -> progress.getLesson() != null)
                .collect(Collectors.toMap(progress -> progress.getLesson().getId(), Function.identity(), (left, right) -> left));

        return lessons.stream()
                .map(lesson -> mapToResponse(lesson, progressByLessonId.get(lesson.getId()), profileId))
                .collect(Collectors.toList());
    }

    private AiTeachingLessonResponse mapToResponse(AiTeachingLesson lesson, AiLessonProgress progress, Long profileId) {
        return AiTeachingLessonResponse.builder()
                .id(lesson.getId())
                .lessonName(lesson.getLessonName())
                .description(lesson.getDescription())
                .reasonableAgeFromMonth(lesson.getReasonableAgeFromMonth())
                .reasonableAgeToMonth(lesson.getReasonableAgeToMonth())
                .recommendedDurationDays(lesson.getRecommendedDurationDays())
                .lessonNote(lesson.getLessonNote())
                .videoUrl(lesson.getVideoUrl())
                .suggestedByOrganization(lesson.getSuggestedByOrganization())
                .sourceUrl(lesson.getSourceUrl())
                .sourceOrganization(lesson.getSourceOrganization())
                .iconUrl(lesson.getIconUrl())
                .sortOrder(lesson.getSortOrder())
                .status(lesson.getStatus())
                .progressId(progress != null ? progress.getId() : null)
                .profileId(profileId)
                .progressStatus(progress != null ? progress.getProgressStatus() : Constants.TABLE_STATUS.INITIATED)
                .startedAt(progress != null ? progress.getStartedAt() : null)
                .completedAt(progress != null ? progress.getCompletedAt() : null)
                .currentDay(progress != null ? progress.getCurrentDay() : null)
                .practiceCount(progress != null ? progress.getPracticeCount() : null)
                .lastPracticedAt(progress != null ? progress.getLastPracticedAt() : null)
                .progressNote(progress != null ? progress.getNote() : null)
                .progressUpdatedAt(progress != null ? progress.getUpdatedAt() : null)
                .build();
    }

    private Profile validateCurrentProfile(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return profile;
        }
        if (profile.getUser() == null || !userDetails.getId().equals(profile.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
        return profile;
    }
}
