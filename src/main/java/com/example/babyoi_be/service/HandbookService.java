package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.request.HandbookCommentRequest;
import com.example.babyoi_be.domain.dto.respone.HandbookCommentResponse;
import com.example.babyoi_be.domain.dto.respone.HandbookPostResponse;

import java.util.List;

public interface HandbookService {
    List<HandbookPostResponse> getPosts(String category, String search);
    HandbookPostResponse getPost(Long postId);
    List<String> getCategories();
    List<HandbookCommentResponse> getComments(Long postId);
    HandbookCommentResponse createComment(Long postId, HandbookCommentRequest request);
}
