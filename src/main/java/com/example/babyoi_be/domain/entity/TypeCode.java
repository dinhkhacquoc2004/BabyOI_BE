package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_code")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    private Long status;
}
