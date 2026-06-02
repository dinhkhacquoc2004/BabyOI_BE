package com.example.babyoi_be.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileAvatarStorageService {
    String uploadAvatar(MultipartFile file);
    void deleteAvatar(String imageUrl);
}
