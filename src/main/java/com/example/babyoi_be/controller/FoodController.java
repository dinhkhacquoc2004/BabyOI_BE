package com.example.babyoi_be.controller;

import com.example.babyoi_be.domain.dto.respone.FoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.FoodResponse;
import com.example.babyoi_be.domain.dto.respone.FavoriteFoodResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.dto.respone.TypeValueResponse;
import com.example.babyoi_be.service.FoodService;
import com.example.babyoi_be.service.TypeValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final TypeValueService typeValueService;

    @GetMapping
    public List<FoodResponse> getFoods() {
        return foodService.getFoods();
    }

    @GetMapping("/filter-options")
    public Map<String, List<TypeValueResponse>> getFilterOptions() {
        return typeValueService.getValuesByCodes(List.of("FOOD_FUNCTION_CODE", "FOOD_ADVICE_FOR"));
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

    @GetMapping("/favorites/profile/{profileId}")
    public List<FoodResponse> getFavoriteFoodsByProfileId(@PathVariable Long profileId) {
        return foodService.getFavoriteFoodsByProfileId(profileId);
    }

    @PostMapping("/favorites/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteFoodResponse addFavoriteFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        return foodService.addFavoriteFood(profileId, foodId);
    }

    @DeleteMapping("/favorites/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavoriteFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        foodService.deleteFavoriteFood(profileId, foodId);
    }

    @GetMapping("/restricted/profile/{profileId}")
    public List<FoodResponse> getRestrictedFoodsByProfileId(@PathVariable Long profileId) {
        return foodService.getRestrictedFoodsByProfileId(profileId);
    }

    @PostMapping("/restricted/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteFoodResponse addRestrictedFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        return foodService.addRestrictedFood(profileId, foodId);
    }

    @DeleteMapping("/restricted/profile/{profileId}/food/{foodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRestrictedFood(
            @PathVariable Long profileId,
            @PathVariable Long foodId) {
        foodService.deleteRestrictedFood(profileId, foodId);
    }

    @GetMapping("/{foodId}/ingredients")
    public List<FoodIngredientResponse> getIngredientsByFoodId(@PathVariable Long foodId) {
        return foodService.getIngredientsByFoodId(foodId);
    }
}
