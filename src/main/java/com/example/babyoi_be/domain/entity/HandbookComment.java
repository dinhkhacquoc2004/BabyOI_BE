package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "handbook_comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandbookComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private HandbookPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private HandbookComment parent;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 120)
    private String userName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "admin_reply")
    private Boolean adminReply;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private Long status;
}
