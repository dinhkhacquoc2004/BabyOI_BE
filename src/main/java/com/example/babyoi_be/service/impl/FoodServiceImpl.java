package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.utils.TextSearchUtils;
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
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.RestrictedFoodRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.FoodService;
import com.example.babyoi_be.service.TypeValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
    private final ProfileRepository profileRepository;
    private final TypeValueService typeValueService;

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
        validateFoodFilters(functionCode, targetAdvanceFor);

        if (!keyword.isBlank()) {
            return searchFoodsInMemory(pageIndex, pageSize, functionCode, targetAdvanceFor, keyword);
        }

        Page<FoodLibrary> foodPage;
        if (functionCode != null && !targetAdvanceFor.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCodeAndAdvanceFor(
                    Constants.TABLE_STATUS.ACTIVE, functionCode, targetAdvanceFor, pageable);
        } else if (!targetAdvanceFor.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndAdvanceFor(Constants.TABLE_STATUS.ACTIVE, targetAdvanceFor, pageable);
        } else if (functionCode != null) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCode(Constants.TABLE_STATUS.ACTIVE, functionCode, pageable);
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

    private PageResponse<FoodResponse> searchFoodsInMemory(
            int pageIndex,
            int pageSize,
            Long functionCode,
            String advanceFor,
            String keyword) {
        List<FoodResponse> matchedFoods = foodLibraryRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .filter(food -> functionCode == null || functionCode.equals(food.getFunctionCode()))
                .filter(food -> advanceFor == null || advanceFor.isBlank() || advanceFor.equals(food.getAdvanceFor()))
                .filter(food -> TextSearchUtils.contains(food.getName(), keyword))
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());

        int totalElements = matchedFoods.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        int fromIndex = Math.min(pageIndex * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        return PageResponse.<FoodResponse>builder()
                .content(matchedFoods.subList(fromIndex, toIndex))
                .page(pageIndex)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(pageIndex == 0)
                .last(totalPages == 0 || pageIndex >= totalPages - 1)
                .build();
    }

    @Override
    public FoodResponse getFoodDetail(Long foodId) {
        FoodLibrary foodLibrary = foodLibraryRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));

        return mapToFoodResponse(foodLibrary, true);
    }

    @Override
    public List<FoodResponse> getFavoriteFoodsByProfileId(Long profileId) {
        validateCurrentProfile(profileId);
        return favoriteFoodRepository.findByProfileId(profileId)
                .stream()
                .map(FavoriteFood::getFoodLibrary)
                .filter(foodLibrary -> foodLibrary != null
                        && Constants.TABLE_STATUS.ACTIVE.equals(foodLibrary.getStatus()))
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FavoriteFoodResponse addFavoriteFood(Long profileId, Long foodId) {
        Profile profile = validateCurrentProfile(profileId);
        FavoriteFood favoriteFood = favoriteFoodRepository.findByProfileIdAndFoodLibraryId(profileId, foodId)
                .orElseGet(() -> createFavoriteFood(profile, foodId));

        return mapToFavoriteFoodResponse(favoriteFood);
    }

    @Override
    @Transactional
    public void deleteFavoriteFood(Long profileId, Long foodId) {
        validateCurrentProfile(profileId);
        FavoriteFood favoriteFood = favoriteFoodRepository.findByProfileIdAndFoodLibraryId(profileId, foodId)
                .orElseThrow(() -> new RuntimeException("Favorite food not found"));

        favoriteFoodRepository.delete(favoriteFood);
    }

    @Override
    public List<FoodResponse> getRestrictedFoodsByProfileId(Long profileId) {
        validateCurrentProfile(profileId);
        return restrictedFoodRepository.findByProfileId(profileId)
                .stream()
                .map(RestrictedFood::getFoodLibrary)
                .filter(foodLibrary -> foodLibrary != null
                        && Constants.TABLE_STATUS.ACTIVE.equals(foodLibrary.getStatus()))
                .map(this::mapToFoodResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FavoriteFoodResponse addRestrictedFood(Long profileId, Long foodId) {
        Profile profile = validateCurrentProfile(profileId);
        RestrictedFood restrictedFood = restrictedFoodRepository.findByProfileIdAndFoodLibraryId(profileId, foodId)
                .orElseGet(() -> createRestrictedFood(profile, foodId));

        return mapToRestrictedFoodResponse(restrictedFood);
    }

    @Override
    @Transactional
    public void deleteRestrictedFood(Long profileId, Long foodId) {
        validateCurrentProfile(profileId);
        RestrictedFood restrictedFood = restrictedFoodRepository.findByProfileIdAndFoodLibraryId(profileId, foodId)
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

    private void validateFoodFilters(Long functionCode, String advanceFor) {
        if (functionCode != null
                && !typeValueService.existsValueNumber("FOOD_FUNCTION_CODE", BigDecimal.valueOf(functionCode))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food function code không hợp lệ");
        }
        if (advanceFor != null
                && !advanceFor.isBlank()
                && !typeValueService.existsValueText("FOOD_ADVICE_FOR", advanceFor)
                && !typeValueService.existsValueCode("FOOD_ADVICE_FOR", advanceFor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food advanceFor không hợp lệ");
        }
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

    private FavoriteFood createFavoriteFood(Profile profile, Long foodId) {
        FoodLibrary foodLibrary = foodLibraryRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));
        String actor = resolveProfileActor(profile);

        FavoriteFood favoriteFood = FavoriteFood.builder()
                .profile(profile)
                .foodLibrary(foodLibrary)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        return favoriteFoodRepository.save(favoriteFood);
    }

    private Profile validateCurrentProfile(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return profile;
        }
        if (profile.getUser() == null || !userDetails.getId().equals(profile.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
        return profile;
    }

    private RestrictedFood createRestrictedFood(Profile profile, Long foodId) {
        FoodLibrary foodLibrary = foodLibraryRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));
        String actor = resolveProfileActor(profile);

        RestrictedFood restrictedFood = RestrictedFood.builder()
                .profile(profile)
                .foodLibrary(foodLibrary)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        return restrictedFoodRepository.save(restrictedFood);
    }

    private String resolveProfileActor(Profile profile) {
        if (profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName().trim();
        }
        return "PROFILE_" + profile.getId();
    }

    private FavoriteFoodResponse mapToFavoriteFoodResponse(FavoriteFood favoriteFood) {
        FoodLibrary foodLibrary = favoriteFood.getFoodLibrary();

        return FavoriteFoodResponse.builder()
                .id(favoriteFood.getId())
                .userId(favoriteFood.getProfile() != null && favoriteFood.getProfile().getUser() != null ? favoriteFood.getProfile().getUser().getId() : null)
                .profileId(favoriteFood.getProfile() != null ? favoriteFood.getProfile().getId() : null)
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
                .userId(restrictedFood.getProfile() != null && restrictedFood.getProfile().getUser() != null ? restrictedFood.getProfile().getUser().getId() : null)
                .profileId(restrictedFood.getProfile() != null ? restrictedFood.getProfile().getId() : null)
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
