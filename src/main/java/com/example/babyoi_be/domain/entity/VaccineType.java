package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "vaccine_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_dose")
    private Integer totalDose;

    @Column(name = "required_age")
    private String requiredAge;

    @Column(name = "for_mother")
    private Boolean forMother;

    @Column(name = "for_child")
    private Boolean forChild;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    private Long status;

    @OneToMany(mappedBy = "vaccineType", cascade = CascadeType.ALL)
    private List<VaccineSchedule> schedules;
}
