package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.utils.TextSearchUtils;
import com.example.babyoi_be.domain.dto.request.HandbookCommentRequest;
import com.example.babyoi_be.domain.dto.respone.HandbookCommentResponse;
import com.example.babyoi_be.domain.dto.respone.HandbookPostResponse;
import com.example.babyoi_be.domain.entity.HandbookComment;
import com.example.babyoi_be.domain.entity.HandbookPost;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.repository.HandbookCommentRepository;
import com.example.babyoi_be.repository.HandbookPostRepository;
import com.example.babyoi_be.service.HandbookService;
import com.example.babyoi_be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HandbookServiceImpl implements HandbookService {

    private final HandbookPostRepository handbookPostRepository;
    private final HandbookCommentRepository handbookCommentRepository;
    private final NotificationService notificationService;

    @Override
    public List<HandbookPostResponse> getPosts(String category, String search) {
        String targetCategory = category != null ? category.trim() : "";
        String keyword = search != null ? search.trim() : "";

        return handbookPostRepository.findByStatusOrderByPublishedAtDescIdDesc(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .filter(post -> targetCategory.isBlank() || targetCategory.equals(post.getCategory()))
                .filter(post -> keyword.isBlank()
                        || TextSearchUtils.contains(post.getTitle(), keyword)
                        || TextSearchUtils.contains(post.getSnippet(), keyword)
                        || TextSearchUtils.contains(post.getContent(), keyword))
                .map(this::mapToPostResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HandbookPostResponse getPost(Long postId) {
        HandbookPost post = getActivePost(postId);
        return mapToPostResponse(post);
    }

    @Override
    public List<String> getCategories() {
        return handbookPostRepository.findByStatusOrderByPublishedAtDescIdDesc(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(HandbookPost::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<HandbookCommentResponse> getComments(Long postId) {
        getActivePost(postId);
        List<HandbookComment> comments = handbookCommentRepository.findByPostIdAndStatusOrderByCreatedAtAscIdAsc(postId, Constants.TABLE_STATUS.ACTIVE);
        Map<Long, List<HandbookComment>> repliesByParentId = comments.stream()
                .filter(comment -> comment.getParent() != null)
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

        return comments.stream()
                .filter(comment -> comment.getParent() == null)
                .map(comment -> mapToCommentResponse(comment, repliesByParentId.getOrDefault(comment.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HandbookCommentResponse createComment(Long postId, HandbookCommentRequest request) {
        HandbookPost post = getActivePost(postId);
        CustomUserDetails currentUser = getCurrentUser();
        HandbookComment parent = null;

        if (request.getParentId() != null) {
            parent = handbookCommentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent comment does not belong to this post");
            }
        }

        HandbookComment comment = HandbookComment.builder()
                .post(post)
                .parent(parent)
                .userId(currentUser.getId())
                .userName(currentUser.getRealName())
                .avatarUrl(null)
                .content(request.getContent().trim())
                .likeCount(0L)
                .adminReply(currentUser.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))
                .createdAt(LocalDateTime.now())
                .status(Constants.TABLE_STATUS.ACTIVE)
                .build();

        HandbookComment savedComment = handbookCommentRepository.save(comment);
        createReplyNotification(post, parent, savedComment, currentUser);

        return mapToCommentResponse(savedComment, List.of());
    }

    private HandbookPost getActivePost(Long postId) {
        HandbookPost post = handbookPostRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Handbook post not found"));
        if (!Constants.TABLE_STATUS.ACTIVE.equals(post.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Handbook post not found");
        }
        return post;
    }

    private CustomUserDetails getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private HandbookPostResponse mapToPostResponse(HandbookPost post) {
        return HandbookPostResponse.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .snippet(post.getSnippet())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .authorName(post.getAuthorName())
                .authorRole(post.getAuthorRole())
                .publishedAt(post.getPublishedAt())
                .commentCount(handbookCommentRepository.countByPostIdAndStatus(post.getId(), Constants.TABLE_STATUS.ACTIVE))
                .build();
    }

    private void createReplyNotification(
            HandbookPost post,
            HandbookComment parent,
            HandbookComment reply,
            CustomUserDetails currentUser
    ) {
        if (parent == null || parent.getUserId() == null || parent.getUserId().equals(currentUser.getId())) {
            return;
        }

        String replierName = safeDisplayName(reply.getUserName(), "Ai đó");
        String postTitle = safeDisplayName(post.getTitle(), "một bài cẩm nang");
        String shortTitle = truncate(postTitle, 48);
        String title = replierName + " đã trả lời bình luận của bạn";
        String body = "Trong bài \"" + shortTitle + "\": " + truncate(reply.getContent(), 120);
        String route = "/camnang/comment?postId=" + post.getId() + "&commentId=" + parent.getId();

        notificationService.createNotification(
                parent.getUserId(),
                Constants.NOTIFICATION_TYPE.HANDBOOK_COMMENT_REPLY,
                title,
                body,
                handbookReplyDataJson(route, post.getId(), postTitle, parent.getId(), reply.getId(), replierName),
                Constants.NOTIFICATION_PRIORITY.NORMAL,
                "HANDBOOK_COMMENT",
                post.getId(),
                true
        );
    }

    private String handbookReplyDataJson(
            String route,
            Long postId,
            String postTitle,
            Long commentId,
            Long replyId,
            String replierName
    ) {
        return "{"
                + "\"screen\":\"handbook_comment\","
                + "\"route\":\"" + escapeJson(route) + "\","
                + "\"postId\":" + postId + ","
                + "\"postTitle\":\"" + escapeJson(postTitle) + "\","
                + "\"commentId\":" + commentId + ","
                + "\"replyId\":" + replyId + ","
                + "\"replierName\":\"" + escapeJson(replierName) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String safeDisplayName(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private HandbookCommentResponse mapToCommentResponse(HandbookComment comment, List<HandbookComment> replies) {
        return HandbookCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .avatarUrl(comment.getAvatarUrl())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .adminReply(comment.getAdminReply())
                .createdAt(comment.getCreatedAt())
                .replies(replies.stream().map(reply -> mapToCommentResponse(reply, List.of())).collect(Collectors.toList()))
                .build();
    }
}
