package com.example.babyoi_be.service;

import com.example.babyoi_be.domain.dto.respone.FoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.IngredientFoodSuggestionResponse;
import com.example.babyoi_be.domain.dto.respone.FoodResponse;
import com.example.babyoi_be.domain.dto.respone.FavoriteFoodResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FoodService {
    List<FoodResponse> getFoods();

    PageResponse<FoodResponse> getFoodsPage(Integer page, Integer size, Long functionCode, String advanceFor, String search);

    FoodResponse getFoodDetail(Long foodId);

    List<FoodResponse> getFavoriteFoodsByProfileId(Long profileId);

    List<Long> getFavoriteFoodIdsByProfileId(Long profileId);

    FavoriteFoodResponse addFavoriteFood(Long profileId, Long foodId);

    void deleteFavoriteFood(Long profileId, Long foodId);

    List<FoodResponse> getRestrictedFoodsByProfileId(Long profileId);

    List<Long> getRestrictedFoodIdsByProfileId(Long profileId);

    FavoriteFoodResponse addRestrictedFood(Long profileId, Long foodId);

    void deleteRestrictedFood(Long profileId, Long foodId);

    List<FoodIngredientResponse> getIngredientsByFoodId(Long foodId);

    IngredientFoodSuggestionResponse suggestFoodsFromIngredients(
            Long profileId,
            List<String> ingredientNames,
            MultipartFile image,
            Integer limit);
}
