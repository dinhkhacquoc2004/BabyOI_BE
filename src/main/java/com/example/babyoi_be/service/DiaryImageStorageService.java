package com.example.babyoi_be.service;

import org.springframework.web.multipart.MultipartFile;

public interface DiaryImageStorageService {
    String uploadImage(MultipartFile file);

    void deleteImage(String imageUrl);
}
