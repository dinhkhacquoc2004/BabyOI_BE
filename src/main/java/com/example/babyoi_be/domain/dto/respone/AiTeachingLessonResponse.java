package com.example.babyoi_be.domain.dto.respone;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTeachingLessonResponse {
    private Long id;
    private String lessonName;
    private String description;
    private Integer reasonableAgeFromMonth;
    private Integer reasonableAgeToMonth;
    private Integer recommendedDurationDays;
    private String lessonNote;
    private String videoUrl;
    private String suggestedByOrganization;
    private String sourceUrl;
    private String sourceOrganization;
    private String iconUrl;
    private Integer sortOrder;
    private Long status;

    private Long progressId;
    private Long profileId;
    private Long progressStatus;
    private LocalDate startedAt;
    private LocalDate completedAt;
    private Integer currentDay;
    private Integer practiceCount;
    private LocalDate lastPracticedAt;
    private String progressNote;
    private LocalDateTime progressUpdatedAt;
}
