package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.DetectedIngredientResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IngredientVisionService {
    boolean isAvailable();

    List<DetectedIngredientResponse> detectIngredients(MultipartFile image);
}
