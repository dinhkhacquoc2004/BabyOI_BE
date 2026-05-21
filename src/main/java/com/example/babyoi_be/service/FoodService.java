package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.FoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.FoodResponse;
import com.example.babyoi_be.domain.dto.respone.FavoriteFoodResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;

import java.util.List;

public interface FoodService {
    List<FoodResponse> getFoods();

    PageResponse<FoodResponse> getFoodsPage(Integer page, Integer size, Long functionCode, String advanceFor, String search);

    FoodResponse getFoodDetail(Long foodId);

    List<FoodResponse> getFavoriteFoodsByUserId(Long userId);

    FavoriteFoodResponse addFavoriteFood(Long userId, Long foodId);

    void deleteFavoriteFood(Long userId, Long foodId);

    List<FoodResponse> getRestrictedFoodsByUserId(Long userId);

    FavoriteFoodResponse addRestrictedFood(Long userId, Long foodId);

    void deleteRestrictedFood(Long userId, Long foodId);

    List<FoodIngredientResponse> getIngredientsByFoodId(Long foodId);
}
