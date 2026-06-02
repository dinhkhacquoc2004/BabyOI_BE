package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "handbook_posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandbookPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String snippet;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "author_name", length = 120)
    private String authorName;

    @Column(name = "author_role", length = 120)
    private String authorRole;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private Long status;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<HandbookComment> comments;
}
