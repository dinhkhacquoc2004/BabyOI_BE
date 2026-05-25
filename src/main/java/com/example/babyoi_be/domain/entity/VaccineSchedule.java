package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vaccine_schedule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_type_id")
    private VaccineType vaccineType;

    @Column(name = "dose_number")
    private Integer doseNumber;

    @Column(name = "offset_value")
    private Integer offsetValue;

    @Column(name = "offset_unit")
    private String offsetUnit; // Enum: DAY, MONTH, YEAR
}