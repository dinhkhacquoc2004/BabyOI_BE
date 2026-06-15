package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.HandbookImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HandbookImageStorageService {
    String uploadImage(MultipartFile file);

    List<HandbookImageResponse> listImages();

    void deleteImage(String imageUrl);
}
