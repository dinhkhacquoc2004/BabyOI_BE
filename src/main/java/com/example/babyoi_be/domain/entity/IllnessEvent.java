package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "illness_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IllnessEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    @Column(name = "end_at")
    private LocalDate endAt;

    @Column(name = "illness_type", nullable = false)
    private String illnessType;

    @Column(name = "status")
    private Long status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
