package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "vaccine_record")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id")
    private Long profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_type_id")
    private VaccineType vaccineType;

    @Column(name = "injection_date")
    private LocalDate injectionDate;

    private String location;

    private String note;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "status")
    private Long status;

}