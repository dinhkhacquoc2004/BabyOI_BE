package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.respone.FoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.FoodResponse;
import com.example.babyoi_be.domain.dto.respone.FavoriteFoodResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public List<FoodResponse> getFoods() {
        return foodService.getFoods();
    }

    @GetMapping("/page")
    public PageResponse<FoodResponse> getFoodsPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long functionCode,
            @RequestParam(required = false) String advanceFor,
            @RequestParam(required = false) String search) {
        return foodService.getFoodsPage(page, size, functionCode, advanceFor, search);
    }

    @GetMapping("/{foodId}")
    public FoodResponse getFoodDetail(@PathVariable Long foodId) {
        return foodService.getFoodDetail(foodId);
    }

    @GetMapping("/detail/{foodId}")
    public FoodResponse getFoodDetailById(@PathVariable Long foodId) {
        return foodService.getFoodDetail(foodId);
    }

    @GetMapping("/favorites/user/{userId}")
    public List<FoodResponse> getFavoriteFoodsByUserId(@PathVariable Long userId) {
        return foodService.getFavoriteFoodsByUserId(userId);
    }

    @PostMapping("/favorites/user/{userId}/food/{foodId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteFoodResponse addFavoriteFood(
            @PathVariable Long userId,
            @PathVariable Long foodId) {
        return foodService.addFavoriteFood(userId, foodId);
    }

    @DeleteMapping("/favorites/user/{userId}/food/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavoriteFood(
            @PathVariable Long userId,
            @PathVariable Long foodId) {
        foodService.deleteFavoriteFood(userId, foodId);
    }

    @GetMapping("/restricted/user/{userId}")
    public List<FoodResponse> getRestrictedFoodsByUserId(@PathVariable Long userId) {
        return foodService.getRestrictedFoodsByUserId(userId);
    }

    @PostMapping("/restricted/user/{userId}/food/{foodId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteFoodResponse addRestrictedFood(
            @PathVariable Long userId,
            @PathVariable Long foodId) {
        return foodService.addRestrictedFood(userId, foodId);
    }

    @DeleteMapping("/restricted/user/{userId}/food/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRestrictedFood(
            @PathVariable Long userId,
            @PathVariable Long foodId) {
        foodService.deleteRestrictedFood(userId, foodId);
    }

    @GetMapping("/{foodId}/ingredients")
    public List<FoodIngredientResponse> getIngredientsByFoodId(@PathVariable Long foodId) {
        return foodService.getIngredientsByFoodId(foodId);
    }
}
