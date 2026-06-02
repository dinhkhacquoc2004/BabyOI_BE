package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.AiTeachingLessonResponse;

import java.util.List;

public interface AiTeachingService {
    List<AiTeachingLessonResponse> getLessonsByProfile(Long profileId);
}
