package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_recommendation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private FoodLibrary foodLibrary;

    @Column(name = "good_points", columnDefinition = "TEXT")
    private String goodPoints;

    @Column(name = "bad_points", columnDefinition = "TEXT")
    private String badPoints;

    @Column(columnDefinition = "TEXT")
    private String advice;

    @Column(name = "cooking_way", columnDefinition = "TEXT")
    private String cookingWay;

    private Long status;
}
