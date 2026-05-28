package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.request.HandbookCommentRequest;
import com.example.babyoi_be.domain.dto.respone.HandbookCommentResponse;
import com.example.babyoi_be.domain.dto.respone.HandbookPostResponse;
import com.example.babyoi_be.service.HandbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/handbook")
@RequiredArgsConstructor
public class HandbookController {

    private final HandbookService handbookService;

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
}
