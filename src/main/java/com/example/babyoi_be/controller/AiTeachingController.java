package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.respone.AiTeachingLessonResponse;
import com.example.babyoi_be.service.AiTeachingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-teaching")
@RequiredArgsConstructor
public class AiTeachingController {

    private final AiTeachingService aiTeachingService;

    @GetMapping("/profiles/{profileId}/lessons")
    public List<AiTeachingLessonResponse> getLessonsByProfile(@PathVariable Long profileId) {
        return aiTeachingService.getLessonsByProfile(profileId);
    }
}
