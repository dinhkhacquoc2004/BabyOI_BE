package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "type_value")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_code_id", nullable = false)
    private TypeCode typeCode;

    @Column(name = "value_code", nullable = false)
    private String valueCode;

    @Column(name = "value_name", nullable = false)
    private String valueName;

    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    @Column(name = "value_number")
    private BigDecimal valueNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "status", nullable = false)
    private Long status;
}
