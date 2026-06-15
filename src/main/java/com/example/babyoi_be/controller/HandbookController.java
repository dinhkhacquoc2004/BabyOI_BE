package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.HandbookCommentRequest;
import com.example.babyoi_be.domain.dto.respone.HandbookCommentResponse;
import com.example.babyoi_be.domain.dto.respone.HandbookImageUploadResponse;
import com.example.babyoi_be.domain.dto.respone.HandbookImageResponse;
import com.example.babyoi_be.domain.dto.respone.HandbookPostResponse;
import com.example.babyoi_be.service.HandbookImageStorageService;
import com.example.babyoi_be.service.HandbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/handbook")
@RequiredArgsConstructor
public class HandbookController {

    private final HandbookService handbookService;
    private final HandbookImageStorageService handbookImageStorageService;

    @GetMapping("/posts")
    public List<HandbookPostResponse> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return handbookService.getPosts(category, search);
    }

    @GetMapping("/posts/{postId}")
    public HandbookPostResponse getPost(@PathVariable Long postId) {
        return handbookService.getPost(postId);
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return handbookService.getCategories();
    }

    @GetMapping("/posts/{postId}/comments")
    public List<HandbookCommentResponse> getComments(@PathVariable Long postId) {
        return handbookService.getComments(postId);
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public HandbookCommentResponse createComment(
            @PathVariable Long postId,
            @Valid @RequestBody HandbookCommentRequest request) {
        return handbookService.createComment(postId, request);
    }

    @PostMapping("/images")
    @PreAuthorize("hasRole('ADMIN')")
    public HandbookImageUploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
        return new HandbookImageUploadResponse(handbookImageStorageService.uploadImage(file));
    }

    @GetMapping("/images")
    @PreAuthorize("hasRole('ADMIN')")
    public List<HandbookImageResponse> getImages() {
        return handbookImageStorageService.listImages();
    }

    @DeleteMapping("/images")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteImage(@RequestParam String imageUrl) {
        handbookImageStorageService.deleteImage(imageUrl);
    }
}
