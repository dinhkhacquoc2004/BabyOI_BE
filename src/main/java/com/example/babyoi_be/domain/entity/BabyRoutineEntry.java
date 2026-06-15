package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "baby_routine_entry",
        indexes = {
                @Index(name = "idx_baby_routine_profile_date", columnList = "profile_id,routine_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BabyRoutineEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "routine_date", nullable = false)
    private LocalDate routineDate;

    @Column(name = "entry_type", nullable = false, length = 30)
    private String type;

    @Column(name = "planned_time", nullable = false)
    private LocalTime plannedTime;

    @Column(name = "actual_time")
    private LocalTime actualTime;

    @Column(name = "activity", nullable = false)
    private String activity;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "icon", length = 40)
    private String icon;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Column(name = "source", length = 30)
    private String source;

    @Column(name = "status")
    private Long status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
