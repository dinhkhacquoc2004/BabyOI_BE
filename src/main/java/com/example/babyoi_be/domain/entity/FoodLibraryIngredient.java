package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_library_ingredients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodLibraryIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_lib_id")
    private FoodLibrary foodLibrary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_ing_id")
    private FoodIngredient foodIngredient;

    @Column(name = "amount_per_serving")
    private Double amountPerServing;

    private String unit;

    private Long status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
