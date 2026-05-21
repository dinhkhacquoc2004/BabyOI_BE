package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.respone.FavoriteFoodResponse;
import com.example.babyoi_be.domain.dto.respone.FoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.FoodNutritionResponse;
import com.example.babyoi_be.domain.dto.respone.FoodRecommendationResponse;
import com.example.babyoi_be.domain.dto.respone.FoodResponse;
import com.example.babyoi_be.domain.dto.respone.IngredientNutritionResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.entity.*;
import com.example.babyoi_be.repository.FavoriteFoodRepository;
import com.example.babyoi_be.repository.FoodLibraryIngredientRepository;
import com.example.babyoi_be.repository.FoodLibraryRepository;
import com.example.babyoi_be.repository.RestrictedFoodRepository;
import com.example.babyoi_be.repository.UsersRepository;
import com.example.babyoi_be.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodServiceImpl implements FoodService {

    private final FoodLibraryRepository foodLibraryRepository;
    private final FavoriteFoodRepository favoriteFoodRepository;
    private final RestrictedFoodRepository restrictedFoodRepository;
    private final FoodLibraryIngredientRepository foodLibraryIngredientRepository;
    private final UsersRepository usersRepository;

    @Override
    public List<FoodResponse> getFoods() {
        return foodLibraryRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<FoodResponse> getFoodsPage(Integer page, Integer size, Long functionCode, String advanceFor, String search) {
        int pageIndex = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 50) : 10;
        String keyword = search != null ? search.trim() : "";
        String targetAdvanceFor = advanceFor != null ? advanceFor.trim() : "";
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "id"));

        Page<FoodLibrary> foodPage;
        if (functionCode != null && !targetAdvanceFor.isBlank() && !keyword.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCodeAndAdvanceForAndNameContainingIgnoreCase(
                    Constants.TABLE_STATUS.ACTIVE, functionCode, targetAdvanceFor, keyword, pageable);
        } else if (functionCode != null && !targetAdvanceFor.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCodeAndAdvanceFor(
                    Constants.TABLE_STATUS.ACTIVE, functionCode, targetAdvanceFor, pageable);
        } else if (!targetAdvanceFor.isBlank() && !keyword.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndAdvanceForAndNameContainingIgnoreCase(
                    Constants.TABLE_STATUS.ACTIVE, targetAdvanceFor, keyword, pageable);
        } else if (!targetAdvanceFor.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndAdvanceFor(Constants.TABLE_STATUS.ACTIVE, targetAdvanceFor, pageable);
        } else if (functionCode != null && !keyword.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCodeAndNameContainingIgnoreCase(
                    Constants.TABLE_STATUS.ACTIVE, functionCode, keyword, pageable);
        } else if (functionCode != null) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCode(Constants.TABLE_STATUS.ACTIVE, functionCode, pageable);
        } else if (!keyword.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndNameContainingIgnoreCase(Constants.TABLE_STATUS.ACTIVE, keyword, pageable);
        } else {
            foodPage = foodLibraryRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE, pageable);
        }

        return PageResponse.<FoodResponse>builder()
                .content(foodPage.getContent().stream().map(this::mapToFoodResponse).collect(Collectors.toList()))
                .page(foodPage.getNumber())
                .size(foodPage.getSize())
                .totalElements(foodPage.getTotalElements())
                .totalPages(foodPage.getTotalPages())
                .first(foodPage.isFirst())
                .last(foodPage.isLast())
                .build();
    }

    @Override
    public FoodResponse getFoodDetail(Long foodId) {
        FoodLibrary foodLibrary = foodLibraryRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));

        return mapToFoodResponse(foodLibrary, true);
    }

    @Override
    public List<FoodResponse> getFavoriteFoodsByUserId(Long userId) {
        return favoriteFoodRepository.findByUserId(userId)
                .stream()
                .map(FavoriteFood::getFoodLibrary)
                .filter(foodLibrary -> foodLibrary != null
                        && Constants.TABLE_STATUS.ACTIVE.equals(foodLibrary.getStatus()))
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FavoriteFoodResponse addFavoriteFood(Long userId, Long foodId) {
        FavoriteFood favoriteFood = favoriteFoodRepository.findByUserIdAndFoodLibraryId(userId, foodId)
                .orElseGet(() -> createFavoriteFood(userId, foodId));

        return mapToFavoriteFoodResponse(favoriteFood);
    }

    @Override
    @Transactional
    public void deleteFavoriteFood(Long userId, Long foodId) {
        FavoriteFood favoriteFood = favoriteFoodRepository.findByUserIdAndFoodLibraryId(userId, foodId)
                .orElseThrow(() -> new RuntimeException("Favorite food not found"));

        favoriteFoodRepository.delete(favoriteFood);
    }

    @Override
    public List<FoodResponse> getRestrictedFoodsByUserId(Long userId) {
        return restrictedFoodRepository.findByUserId(userId)
                .stream()
                .map(RestrictedFood::getFoodLibrary)
                .filter(foodLibrary -> foodLibrary != null
                        && Constants.TABLE_STATUS.ACTIVE.equals(foodLibrary.getStatus()))
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FavoriteFoodResponse addRestrictedFood(Long userId, Long foodId) {
        RestrictedFood restrictedFood = restrictedFoodRepository.findByUserIdAndFoodLibraryId(userId, foodId)
                .orElseGet(() -> createRestrictedFood(userId, foodId));

        return mapToRestrictedFoodResponse(restrictedFood);
    }

    @Override
    @Transactional
    public void deleteRestrictedFood(Long userId, Long foodId) {
        RestrictedFood restrictedFood = restrictedFoodRepository.findByUserIdAndFoodLibraryId(userId, foodId)
                .orElseThrow(() -> new RuntimeException("Restricted food not found"));

        restrictedFoodRepository.delete(restrictedFood);
    }

    @Override
    public List<FoodIngredientResponse> getIngredientsByFoodId(Long foodId) {
        return foodLibraryIngredientRepository.findByFoodLibraryId(foodId)
                .stream()
                .filter(ingredient -> !Constants.TABLE_STATUS.DELETED.equals(ingredient.getStatus()))
                .map(this::mapToIngredientResponse)
                .collect(Collectors.toList());
    }

    private FoodResponse mapToFoodResponse(FoodLibrary foodLibrary) {
        return mapToFoodResponse(foodLibrary, false);
    }

    private FoodResponse mapToFoodResponse(FoodLibrary foodLibrary, boolean includeIngredients) {
        return FoodResponse.builder()
                .id(foodLibrary.getId())
                .functionCode(foodLibrary.getFunctionCode())
                .name(foodLibrary.getName())
                .imageUrl(foodLibrary.getImageUrl())
                .advanceFor(foodLibrary.getAdvanceFor())
                .status(foodLibrary.getStatus())
                .createdAt(foodLibrary.getCreatedAt())
                .updatedAt(foodLibrary.getUpdatedAt())
                .createdBy(foodLibrary.getCreatedBy())
                .updatedBy(foodLibrary.getUpdatedBy())
                .nutrition(mapToNutritionResponse(foodLibrary.getNutritionSummary()))
                .recommendation(mapToRecommendationResponse(foodLibrary.getRecommendation()))
                .ingredients(includeIngredients ? getIngredientsByFoodId(foodLibrary.getId()) : null)
                .build();
    }

    private FavoriteFood createFavoriteFood(Long userId, Long foodId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        FoodLibrary foodLibrary = foodLibraryRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));

        FavoriteFood favoriteFood = FavoriteFood.builder()
                .user(user)
                .foodLibrary(foodLibrary)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(String.valueOf(userId))
                .updatedBy(String.valueOf(userId))
                .build();

        return favoriteFoodRepository.save(favoriteFood);
    }

    private RestrictedFood createRestrictedFood(Long userId, Long foodId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        FoodLibrary foodLibrary = foodLibraryRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));

        RestrictedFood restrictedFood = RestrictedFood.builder()
                .user(user)
                .foodLibrary(foodLibrary)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(String.valueOf(userId))
                .updatedBy(String.valueOf(userId))
                .build();

        return restrictedFoodRepository.save(restrictedFood);
    }

    private FavoriteFoodResponse mapToFavoriteFoodResponse(FavoriteFood favoriteFood) {
        FoodLibrary foodLibrary = favoriteFood.getFoodLibrary();

        return FavoriteFoodResponse.builder()
                .id(favoriteFood.getId())
                .userId(favoriteFood.getUser() != null ? favoriteFood.getUser().getId() : null)
                .foodId(foodLibrary != null ? foodLibrary.getId() : null)
                .createdAt(favoriteFood.getCreatedAt())
                .updatedAt(favoriteFood.getUpdatedAt())
                .createdBy(favoriteFood.getCreatedBy())
                .updatedBy(favoriteFood.getUpdatedBy())
                .food(foodLibrary != null ? mapToFoodResponse(foodLibrary) : null)
                .build();
    }

    private FavoriteFoodResponse mapToRestrictedFoodResponse(RestrictedFood restrictedFood) {
        FoodLibrary foodLibrary = restrictedFood.getFoodLibrary();

        return FavoriteFoodResponse.builder()
                .id(restrictedFood.getId())
                .userId(restrictedFood.getUser() != null ? restrictedFood.getUser().getId() : null)
                .foodId(foodLibrary != null ? foodLibrary.getId() : null)
                .createdAt(restrictedFood.getCreatedAt())
                .updatedAt(restrictedFood.getUpdatedAt())
                .createdBy(restrictedFood.getCreatedBy())
                .updatedBy(restrictedFood.getUpdatedBy())
                .food(foodLibrary != null ? mapToFoodResponse(foodLibrary) : null)
                .build();
    }

    private FoodNutritionResponse mapToNutritionResponse(FoodNutritionSummary nutrition) {
        if (nutrition == null) {
            return null;
        }

        return FoodNutritionResponse.builder()
                .id(nutrition.getId())
                .totalCalories(nutrition.getTotalCalories())
                .totalCaloriesUnit(nutrition.getTotalCaloriesUnit())
                .totalProtein(nutrition.getTotalProtein())
                .totalProteinUnit(nutrition.getTotalProteinUnit())
                .totalCarbs(nutrition.getTotalCarbs())
                .totalCarbsUnit(nutrition.getTotalCarbsUnit())
                .totalFat(nutrition.getTotalFat())
                .totalFatUnit(nutrition.getTotalFatUnit())
                .totalFiber(nutrition.getTotalFiber())
                .totalFiberUnit(nutrition.getTotalFiberUnit())
                .totalSugar(nutrition.getTotalSugar())
                .totalSugarUnit(nutrition.getTotalSugarUnit())
                .totalSodium(nutrition.getTotalSodium())
                .totalSodiumUnit(nutrition.getTotalSodiumUnit())
                .build();
    }

    private FoodRecommendationResponse mapToRecommendationResponse(FoodRecommendation recommendation) {
        if (recommendation == null) {
            return null;
        }

        return FoodRecommendationResponse.builder()
                .id(recommendation.getId())
                .goodPoints(recommendation.getGoodPoints())
                .badPoints(recommendation.getBadPoints())
                .advice(recommendation.getAdvice())
                .cookingWay(recommendation.getCookingWay())
                .status(recommendation.getStatus())
                .build();
    }

    private FoodIngredientResponse mapToIngredientResponse(FoodLibraryIngredient ingredient) {
        FoodIngredient foodIngredient = ingredient.getFoodIngredient();

        return FoodIngredientResponse.builder()
                .id(ingredient.getId())
                .foodIngredientId(foodIngredient != null ? foodIngredient.getId() : null)
                .nameIngredients(foodIngredient != null ? foodIngredient.getNameIngredients() : null)
                .imageUrl(foodIngredient != null ? foodIngredient.getImageUrl() : null)
                .amountPerServing(ingredient.getAmountPerServing())
                .unit(ingredient.getUnit())
                .status(ingredient.getStatus())
                .description(ingredient.getDescription())
                .nutrition(mapToIngredientNutritionResponse(foodIngredient != null ? foodIngredient.getNutrition() : null))
                .build();
    }

    private IngredientNutritionResponse mapToIngredientNutritionResponse(IngredientNutrition nutrition) {
        if (nutrition == null) {
            return null;
        }

        return IngredientNutritionResponse.builder()
                .id(nutrition.getId())
                .baseAmount(nutrition.getBaseAmount())
                .baseUnit(nutrition.getBaseUnit())
                .calories(nutrition.getCalories())
                .caloriesUnit(nutrition.getCaloriesUnit())
                .protein(nutrition.getProtein())
                .proteinUnit(nutrition.getProteinUnit())
                .carbs(nutrition.getCarbs())
                .carbsUnit(nutrition.getCarbsUnit())
                .fat(nutrition.getFat())
                .fatUnit(nutrition.getFatUnit())
                .fiber(nutrition.getFiber())
                .fiberUnit(nutrition.getFiberUnit())
                .sugar(nutrition.getSugar())
                .sugarUnit(nutrition.getSugarUnit())
                .sodium(nutrition.getSodium())
                .sodiumUnit(nutrition.getSodiumUnit())
                .status(nutrition.getStatus())
                .build();
    }
}
