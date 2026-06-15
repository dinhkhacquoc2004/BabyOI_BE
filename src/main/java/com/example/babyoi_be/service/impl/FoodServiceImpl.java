package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.common.utils.TextSearchUtils;
import com.example.babyoi_be.domain.dto.respone.DetectedIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.FavoriteFoodResponse;
import com.example.babyoi_be.domain.dto.respone.FoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.FoodNutritionResponse;
import com.example.babyoi_be.domain.dto.respone.FoodRecommendationResponse;
import com.example.babyoi_be.domain.dto.respone.FoodResponse;
import com.example.babyoi_be.domain.dto.respone.IngredientFoodSuggestionItemResponse;
import com.example.babyoi_be.domain.dto.respone.IngredientFoodSuggestionResponse;
import com.example.babyoi_be.domain.dto.respone.IngredientNutritionResponse;
import com.example.babyoi_be.domain.dto.respone.MatchedFoodIngredientResponse;
import com.example.babyoi_be.domain.dto.respone.PageResponse;
import com.example.babyoi_be.domain.entity.*;
import com.example.babyoi_be.repository.FavoriteFoodRepository;
import com.example.babyoi_be.repository.FoodIngredientRepository;
import com.example.babyoi_be.repository.FoodLibraryIngredientRepository;
import com.example.babyoi_be.repository.FoodLibraryRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.RestrictedFoodRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.FoodService;
import com.example.babyoi_be.service.IngredientVisionService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodServiceImpl implements FoodService {

    private static final Map<String, List<String>> ADVANCE_FOR_ALIASES = Map.of(
            "FOR_BABY_6_8_MONTHS", List.of("FOR_BABY_6_8_MONTHS", "FOR_BABY_6_8_MONTHS_DEVELOPMENT"),
            "FOR_BABY_9_11_MONTHS", List.of("FOR_BABY_9_11_MONTHS", "FOR_BABY_9_11_MONTHS_DEVELOPMENT"),
            "FOR_BABY_12_18_MONTHS", List.of("FOR_BABY_12_18_MONTHS", "FOR_BABY_12_18_MONTHS_DEVELOPMENT"),
            "FOR_BABY_19_24_MONTHS", List.of("FOR_BABY_19_24_MONTHS", "FOR_BABY_19_24_MONTHS_DEVELOPMENT")
    );
    private static final Set<String> INGREDIENT_MATCH_STOP_WORDS = Set.of(
            "nguyen", "con", "nguyencon", "song", "chin", "tuoi", "dong", "lanh",
            "cat", "khuc", "mieng", "phan", "nguyenlieu", "lieu"
    );
    private static final Set<String> SINGLE_TOKEN_ALIAS_ONLY = Set.of("ga", "heo", "lon", "vit", "tom");
    private static final Map<String, List<String>> INGREDIENT_FAMILY_ALIASES = Map.ofEntries(
            Map.entry("ga", List.of("thit ga", "uc ga", "uc ga xay", "gan ga", "nuoc dung ga nhat")),
            Map.entry("thit ga", List.of("thit ga", "uc ga", "uc ga xay", "gan ga", "nuoc dung ga nhat")),
            Map.entry("ga ta", List.of("thit ga", "uc ga", "uc ga xay", "gan ga", "nuoc dung ga nhat")),
            Map.entry("ga nguyen con", List.of("thit ga", "uc ga", "uc ga xay", "gan ga", "nuoc dung ga nhat")),
            Map.entry("heo", List.of("thit heo", "thit heo nac", "thit heo xay")),
            Map.entry("lon", List.of("thit heo", "thit heo nac", "thit heo xay")),
            Map.entry("thit heo", List.of("thit heo", "thit heo nac", "thit heo xay")),
            Map.entry("thit lon", List.of("thit heo", "thit heo nac", "thit heo xay")),
            Map.entry("tom", List.of("tom")),
            Map.entry("vit", List.of("uc vit bo da")),
            Map.entry("thit vit", List.of("uc vit bo da")),
            Map.entry("trung", List.of("trung ga", "long do trung")),
            Map.entry("trung ga", List.of("trung ga", "long do trung"))
    );
    private static final Map<String, String> VIETNAMESE_INGREDIENT_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("ca", "Cá"),
            Map.entry("ca hoi", "Cá hồi"),
            Map.entry("ca thu", "Cá thu"),
            Map.entry("ca trang", "Cá trắng"),
            Map.entry("ca chua", "Cà chua"),
            Map.entry("ca tim", "Cà tím"),
            Map.entry("chanh", "Chanh"),
            Map.entry("toi", "Tỏi"),
            Map.entry("rau mui", "Rau mùi"),
            Map.entry("rau bina", "Rau bina"),
            Map.entry("cai bo xoi", "Cải bó xôi"),
            Map.entry("dau do", "Đậu đỏ"),
            Map.entry("dau den", "Đậu đen"),
            Map.entry("dau xanh", "Đậu xanh"),
            Map.entry("dau lang", "Đậu lăng"),
            Map.entry("dau lang do", "Đậu lăng đỏ"),
            Map.entry("dau an dam", "Dầu ăn dặm"),
            Map.entry("ga", "Gà"),
            Map.entry("ga nguyen con", "Gà nguyên con"),
            Map.entry("thit ga", "Thịt gà"),
            Map.entry("uc ga", "Ức gà"),
            Map.entry("uc ga xay", "Ức gà xay"),
            Map.entry("gan ga", "Gan gà"),
            Map.entry("trung", "Trứng"),
            Map.entry("trung ga", "Trứng gà"),
            Map.entry("long do trung", "Lòng đỏ trứng"),
            Map.entry("tom", "Tôm"),
            Map.entry("thit bo", "Thịt bò"),
            Map.entry("thit bo nac", "Thịt bò nạc"),
            Map.entry("thit bo xay", "Thịt bò xay"),
            Map.entry("thit heo", "Thịt heo"),
            Map.entry("thit heo nac", "Thịt heo nạc"),
            Map.entry("thit heo xay", "Thịt heo xay")
    );

    private final FoodLibraryRepository foodLibraryRepository;
    private final FavoriteFoodRepository favoriteFoodRepository;
    private final RestrictedFoodRepository restrictedFoodRepository;
    private final FoodIngredientRepository foodIngredientRepository;
    private final FoodLibraryIngredientRepository foodLibraryIngredientRepository;
    private final ProfileRepository profileRepository;
    private final TypeValueService typeValueService;
    private final IngredientVisionService ingredientVisionService;

    @Override
    public List<FoodResponse> getFoods() {
        return foodLibraryRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE)
                .stream()
                .map(this::mapToFoodListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<FoodResponse> getFoodsPage(Integer page, Integer size, Long functionCode, String advanceFor, String search) {
        int pageIndex = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? Math.min(size, 50) : 10;
        String keyword = search != null ? search.trim() : "";
        String targetAdvanceFor = advanceFor != null ? advanceFor.trim() : "";
        List<String> targetAdvanceForValues = resolveAdvanceForValues(targetAdvanceFor);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        validateFoodFilters(functionCode, targetAdvanceFor);

        Page<FoodLibrary> foodPage;
        if (!keyword.isBlank() && functionCode != null && !targetAdvanceForValues.isEmpty()) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCodeAndAdvanceForInAndNameContainingIgnoreCase(
                    Constants.TABLE_STATUS.ACTIVE, functionCode, targetAdvanceForValues, keyword, pageable);
        } else if (!keyword.isBlank() && functionCode != null) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCodeAndNameContainingIgnoreCase(
                    Constants.TABLE_STATUS.ACTIVE, functionCode, keyword, pageable);
        } else if (!keyword.isBlank() && !targetAdvanceForValues.isEmpty()) {
            foodPage = foodLibraryRepository.findByStatusAndAdvanceForInAndNameContainingIgnoreCase(
                    Constants.TABLE_STATUS.ACTIVE, targetAdvanceForValues, keyword, pageable);
        } else if (!keyword.isBlank()) {
            foodPage = foodLibraryRepository.findByStatusAndNameContainingIgnoreCase(
                    Constants.TABLE_STATUS.ACTIVE, keyword, pageable);
        } else if (functionCode != null && !targetAdvanceForValues.isEmpty()) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCodeAndAdvanceForIn(
                    Constants.TABLE_STATUS.ACTIVE, functionCode, targetAdvanceForValues, pageable);
        } else if (!targetAdvanceForValues.isEmpty()) {
            foodPage = foodLibraryRepository.findByStatusAndAdvanceForIn(
                    Constants.TABLE_STATUS.ACTIVE, targetAdvanceForValues, pageable);
        } else if (functionCode != null) {
            foodPage = foodLibraryRepository.findByStatusAndFunctionCode(Constants.TABLE_STATUS.ACTIVE, functionCode, pageable);
        } else {
            foodPage = foodLibraryRepository.findByStatus(Constants.TABLE_STATUS.ACTIVE, pageable);
        }

        return PageResponse.<FoodResponse>builder()
                .content(foodPage.getContent().stream().map(this::mapToFoodListResponse).collect(Collectors.toList()))
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
    public List<FoodResponse> getFavoriteFoodsByProfileId(Long profileId) {
        validateCurrentProfile(profileId);
        return favoriteFoodRepository.findByProfileId(profileId)
                .stream()
                .map(FavoriteFood::getFoodLibrary)
                .filter(foodLibrary -> foodLibrary != null
                        && Constants.TABLE_STATUS.ACTIVE.equals(foodLibrary.getStatus()))
                .map(this::mapToFoodListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getFavoriteFoodIdsByProfileId(Long profileId) {
        validateCurrentProfile(profileId);
        return favoriteFoodRepository.findFoodLibraryIdsByProfileId(profileId);
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
                .map(this::mapToFoodListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getRestrictedFoodIdsByProfileId(Long profileId) {
        validateCurrentProfile(profileId);
        return restrictedFoodRepository.findFoodLibraryIdsByProfileId(profileId);
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

    @Override
    public IngredientFoodSuggestionResponse suggestFoodsFromIngredients(
            Long profileId,
            List<String> ingredientNames,
            MultipartFile image,
            Integer limit) {
        int suggestionLimit = limit != null && limit > 0 ? Math.min(limit, 20) : 5;
        List<String> warnings = new ArrayList<>();
        Profile profile = profileId != null ? validateCurrentProfile(profileId) : null;
        List<String> manualIngredients = cleanIngredientNames(ingredientNames);
        boolean imageProvided = image != null && !image.isEmpty();
        boolean imageAnalyzed = false;
        List<DetectedIngredientResponse> detectedIngredients = List.of();

        if (imageProvided) {
            if (ingredientVisionService.isAvailable()) {
                try {
                    detectedIngredients = ingredientVisionService.detectIngredients(image)
                            .stream()
                            .map(this::restoreDetectedIngredientDisplayName)
                            .collect(Collectors.toList());
                    imageAnalyzed = true;
                } catch (ResponseStatusException exception) {
                    if (manualIngredients.isEmpty()) {
                        throw exception;
                    }
                    warnings.add("AI vision không phân tích được ảnh, hệ thống chỉ dùng ingredientNames để gợi ý món.");
                }
            } else if (manualIngredients.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần cấu hình GEMINI_API_KEY hoặc gửi ingredientNames");
            } else {
                warnings.add("AI vision chưa được cấu hình, hệ thống chỉ dùng ingredientNames để gợi ý món.");
            }
        }

        List<String> requestedIngredients = mergeIngredientNames(manualIngredients, detectedIngredients);
        if (requestedIngredients.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần gửi ảnh nguyên liệu hoặc ingredientNames");
        }
        List<FoodIngredient> ingredientCatalog = foodIngredientRepository.findAvailableIngredients(Constants.TABLE_STATUS.DELETED);
        List<MatchedFoodIngredientResponse> matchedDbIngredients = matchIngredientsInCatalog(
                requestedIngredients,
                detectedIngredients,
                ingredientCatalog);
        Set<Long> matchedIngredientIds = matchedDbIngredients.stream()
                .map(MatchedFoodIngredientResponse::getIngredientId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (matchedIngredientIds.isEmpty()) {
            warnings.add(imageAnalyzed
                    ? "AI đã nhận diện nguyên liệu nhưng chưa tìm thấy nguyên liệu trùng khớp trong bảng food_ingredients."
                    : "Chưa tìm thấy nguyên liệu trùng khớp trong bảng food_ingredients.");
        }

        Integer ageMonths = calculateAgeMonths(profile);
        String targetAdvanceFor = resolveTargetAdvanceFor(ageMonths);
        if (ageMonths != null && ageMonths < 6) {
            warnings.add("Profile dưới 6 tháng tuổi, nên ưu tiên sữa mẹ/sữa công thức và hỏi chuyên gia trước khi ăn dặm.");
        }

        Set<Long> restrictedFoodIds = profileId != null
                ? new HashSet<>(restrictedFoodRepository.findFoodLibraryIdsByProfileId(profileId))
                : Set.of();
        List<FoodLibraryIngredient> recipeIngredients = foodLibraryIngredientRepository.findActiveRecipeIngredients(
                Constants.TABLE_STATUS.ACTIVE,
                Constants.TABLE_STATUS.DELETED);

        List<IngredientFoodSuggestionItemResponse> suggestions = buildIngredientSuggestions(
                recipeIngredients,
                matchedIngredientIds,
                restrictedFoodIds,
                targetAdvanceFor,
                suggestionLimit);

        if (!restrictedFoodIds.isEmpty()) {
            warnings.add("Đã loại bỏ các món nằm trong danh sách hạn chế của profile.");
        }
        if (suggestions.isEmpty()) {
            warnings.add("Chưa tìm thấy món phù hợp trong thư viện hiện tại từ các nguyên liệu đã nhận diện.");
        }

        return IngredientFoodSuggestionResponse.builder()
                .profileId(profile != null ? profile.getId() : null)
                .profileName(profile != null ? profile.getName() : null)
                .profileAgeMonths(ageMonths)
                .imageAnalyzed(imageAnalyzed)
                .detectedIngredients(detectedIngredients)
                .matchedDbIngredients(matchedDbIngredients)
                .inputIngredients(manualIngredients)
                .normalizedIngredients(requestedIngredients.stream()
                        .map(this::restoreVietnameseIngredientDisplayName)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .collect(Collectors.toList()))
                .suggestions(suggestions)
                .warnings(warnings)
                .disclaimer("Thông tin dinh dưỡng chỉ là ước tính tham khảo, không thay thế bác sĩ hoặc chuyên gia dinh dưỡng.")
                .build();
    }

    private List<IngredientFoodSuggestionItemResponse> buildIngredientSuggestions(
            List<FoodLibraryIngredient> recipeIngredients,
            Set<Long> matchedIngredientIds,
            Set<Long> restrictedFoodIds,
            String targetAdvanceFor,
            int suggestionLimit) {
        Map<Long, FoodSuggestionCandidate> candidates = new LinkedHashMap<>();
        for (FoodLibraryIngredient recipeIngredient : recipeIngredients) {
            FoodLibrary foodLibrary = recipeIngredient.getFoodLibrary();
            FoodIngredient foodIngredient = recipeIngredient.getFoodIngredient();
            if (foodLibrary == null || foodLibrary.getId() == null || foodIngredient == null) {
                continue;
            }
            FoodSuggestionCandidate candidate = candidates.computeIfAbsent(
                    foodLibrary.getId(),
                    ignored -> new FoodSuggestionCandidate(foodLibrary));
            String recipeIngredientName = foodIngredient.getNameIngredients();
            if (recipeIngredientName == null || recipeIngredientName.isBlank()) {
                continue;
            }
            candidate.addRecipeIngredient(recipeIngredientName);
            if (matchedIngredientIds.contains(foodIngredient.getId())) {
                candidate.addMatchedIngredient(recipeIngredientName);
            }
        }

        return candidates.values().stream()
                .filter(candidate -> !restrictedFoodIds.contains(candidate.foodLibrary().getId()))
                .filter(candidate -> !candidate.matchedIngredients().isEmpty())
                .peek(candidate -> candidate.calculateScore(targetAdvanceFor))
                .sorted(Comparator.comparing(FoodSuggestionCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.matchedIngredients().size(), Comparator.reverseOrder())
                        .thenComparing(candidate -> candidate.foodLibrary().getId()))
                .limit(suggestionLimit)
                .map(candidate -> IngredientFoodSuggestionItemResponse.builder()
                        .food(mapToFoodListResponse(candidate.foodLibrary()))
                        .matchScore(roundScore(candidate.score()))
                        .matchedIngredients(new ArrayList<>(candidate.matchedIngredients()))
                        .missingIngredients(candidate.missingIngredients())
                        .suitabilityNote(buildSuitabilityNote(candidate.foodLibrary(), targetAdvanceFor))
                        .nutritionNote(buildNutritionNote(candidate.foodLibrary().getNutritionSummary()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> cleanIngredientNames(List<String> ingredientNames) {
        if (ingredientNames == null) {
            return List.of();
        }
        LinkedHashMap<String, String> uniqueIngredients = new LinkedHashMap<>();
        for (String value : ingredientNames) {
            if (value == null) {
                continue;
            }
            for (String ingredientName : value.split("[,;\\n]")) {
                String cleanedName = ingredientName.trim();
                String normalizedName = TextSearchUtils.normalizeSearchText(cleanedName);
                if (!cleanedName.isBlank() && !normalizedName.isBlank()) {
                    uniqueIngredients.putIfAbsent(normalizedName, restoreVietnameseIngredientDisplayName(cleanedName));
                }
            }
        }
        return new ArrayList<>(uniqueIngredients.values());
    }

    private DetectedIngredientResponse restoreDetectedIngredientDisplayName(DetectedIngredientResponse ingredient) {
        if (ingredient == null) {
            return null;
        }
        return DetectedIngredientResponse.builder()
                .name(restoreVietnameseIngredientDisplayName(ingredient.getName()))
                .confidence(ingredient.getConfidence())
                .build();
    }

    private String restoreVietnameseIngredientDisplayName(String ingredientName) {
        if (ingredientName == null || ingredientName.isBlank()) {
            return "";
        }
        String normalizedName = TextSearchUtils.normalizeSearchText(ingredientName)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return VIETNAMESE_INGREDIENT_DISPLAY_NAMES.getOrDefault(normalizedName, ingredientName.trim());
    }

    private List<String> mergeIngredientNames(
            List<String> manualIngredients,
            List<DetectedIngredientResponse> detectedIngredients) {
        LinkedHashMap<String, String> mergedIngredients = new LinkedHashMap<>();
        manualIngredients.forEach(ingredient -> {
            String normalizedIngredient = TextSearchUtils.normalizeSearchText(ingredient);
            if (!normalizedIngredient.isBlank()) {
                mergedIngredients.putIfAbsent(normalizedIngredient, ingredient);
            }
        });
        detectedIngredients.forEach(ingredient -> {
            if (ingredient == null || ingredient.getName() == null) {
                return;
            }
            String ingredientName = ingredient.getName().trim();
            String normalizedIngredient = TextSearchUtils.normalizeSearchText(ingredientName);
            if (!normalizedIngredient.isBlank()) {
                mergedIngredients.putIfAbsent(normalizedIngredient, ingredientName);
            }
        });
        return new ArrayList<>(mergedIngredients.values());
    }

    private List<MatchedFoodIngredientResponse> matchIngredientsInCatalog(
            List<String> requestedIngredients,
            List<DetectedIngredientResponse> detectedIngredients,
            List<FoodIngredient> ingredientCatalog) {
        Map<String, Double> confidenceByName = detectedIngredients.stream()
                .filter(ingredient -> ingredient != null && ingredient.getName() != null)
                .collect(Collectors.toMap(
                        ingredient -> TextSearchUtils.normalizeSearchText(ingredient.getName()),
                        ingredient -> ingredient.getConfidence() != null ? ingredient.getConfidence() : 0.0,
                        Math::max,
                        LinkedHashMap::new));
        LinkedHashMap<Long, MatchedFoodIngredientResponse> matchedIngredients = new LinkedHashMap<>();

        for (String requestedIngredient : requestedIngredients) {
            String normalizedRequested = TextSearchUtils.normalizeSearchText(requestedIngredient);
            double confidence = confidenceByName.getOrDefault(normalizedRequested, 1.0);
            for (FoodIngredient catalogIngredient : ingredientCatalog) {
                double score = scoreIngredientNameMatch(requestedIngredient, catalogIngredient.getNameIngredients());
                if (score < 0.65) {
                    continue;
                }
                matchedIngredients.putIfAbsent(catalogIngredient.getId(), MatchedFoodIngredientResponse.builder()
                        .detectedName(restoreVietnameseIngredientDisplayName(requestedIngredient))
                        .confidence(confidence)
                        .ingredientId(catalogIngredient.getId())
                        .ingredientName(catalogIngredient.getNameIngredients())
                        .imageUrl(catalogIngredient.getImageUrl())
                        .nutrition(mapToIngredientNutritionResponse(catalogIngredient.getNutrition()))
                        .build());
            }
        }

        return new ArrayList<>(matchedIngredients.values());
    }

    private double scoreIngredientNameMatch(String requestedIngredient, String catalogIngredient) {
        String requested = normalizeIngredientMatchText(requestedIngredient);
        String catalog = normalizeIngredientMatchText(catalogIngredient);
        if (requested.isBlank() || catalog.isBlank()) {
            return 0.0;
        }
        if (requested.equals(catalog)) {
            return 1.0;
        }
        if (isIngredientFamilyAliasMatch(requested, catalog)) {
            return 0.82;
        }
        if (containsIngredientPhrase(catalog, requested)) {
            return 0.85;
        }
        if (containsIngredientPhrase(requested, catalog)) {
            return 0.75;
        }
        return scoreIngredientTokenOverlap(requested, catalog);
    }

    private boolean isIngredientFamilyAliasMatch(String requested, String catalog) {
        List<String> aliases = INGREDIENT_FAMILY_ALIASES.get(requested);
        return aliases != null && aliases.contains(catalog);
    }

    private double scoreIngredientTokenOverlap(String requested, String catalog) {
        Set<String> requestedTokens = ingredientMatchTokens(requested);
        Set<String> catalogTokens = ingredientMatchTokens(catalog);
        if (requestedTokens.isEmpty() || catalogTokens.isEmpty()) {
            return 0.0;
        }
        if (requestedTokens.size() == 1) {
            String onlyToken = requestedTokens.iterator().next();
            return SINGLE_TOKEN_ALIAS_ONLY.contains(onlyToken) ? 0.0 : scoreSingleIngredientToken(onlyToken, catalogTokens);
        }

        long commonTokens = requestedTokens.stream()
                .filter(catalogTokens::contains)
                .count();
        if (commonTokens == 0) {
            return 0.0;
        }
        double requestedCoverage = (double) commonTokens / requestedTokens.size();
        double catalogCoverage = (double) commonTokens / catalogTokens.size();
        if (requestedCoverage >= 0.66 || catalogCoverage >= 0.66) {
            return 0.68;
        }
        return 0.0;
    }

    private double scoreSingleIngredientToken(String token, Set<String> catalogTokens) {
        if (token.length() < 4 || !catalogTokens.contains(token)) {
            return 0.0;
        }
        return 0.66;
    }

    private Set<String> ingredientMatchTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return List.of(value.split("\\s+"))
                .stream()
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .filter(token -> !INGREDIENT_MATCH_STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeIngredientMatchText(String value) {
        String normalized = TextSearchUtils.normalizeSearchText(value)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return "";
        }
        return ingredientMatchTokens(normalized).stream()
                .collect(Collectors.joining(" "));
    }

    private boolean containsIngredientPhrase(String source, String phrase) {
        if (phrase.length() < 4) {
            return false;
        }
        return (" " + source + " ").contains(" " + phrase + " ") || source.contains(phrase);
    }

    private Integer calculateAgeMonths(Profile profile) {
        if (profile == null || profile.getDateOfBirth() == null || profile.getDateOfBirth().isAfter(LocalDate.now())) {
            return null;
        }
        Period age = Period.between(profile.getDateOfBirth(), LocalDate.now());
        return age.getYears() * 12 + age.getMonths();
    }

    private String resolveTargetAdvanceFor(Integer ageMonths) {
        if (ageMonths == null) {
            return null;
        }
        if (ageMonths >= 6 && ageMonths <= 8) {
            return Constants.FOOD_ADVICE_FOR.BABY_6_TO_8_MONTHS;
        }
        if (ageMonths >= 9 && ageMonths <= 11) {
            return Constants.FOOD_ADVICE_FOR.BABY_9_TO_11_MONTHS;
        }
        if (ageMonths >= 12 && ageMonths <= 18) {
            return Constants.FOOD_ADVICE_FOR.BABY_12_TO_18_MONTHS;
        }
        if (ageMonths >= 19 && ageMonths <= 24) {
            return Constants.FOOD_ADVICE_FOR.BABY_19_TO_24_MONTHS;
        }
        return null;
    }

    private String buildSuitabilityNote(FoodLibrary foodLibrary, String targetAdvanceFor) {
        if (targetAdvanceFor == null) {
            return "Gợi ý dựa trên nguyên liệu trùng khớp trong thư viện món ăn.";
        }
        if (resolveAdvanceForValues(targetAdvanceFor).contains(foodLibrary.getAdvanceFor())) {
            return "Phù hợp nhóm tuổi ăn dặm của profile theo advanceFor.";
        }
        return "Có nguyên liệu phù hợp, nhưng cần kiểm tra lại độ tuổi/khẩu phần trước khi dùng.";
    }

    private String buildNutritionNote(FoodNutritionSummary nutrition) {
        if (nutrition == null) {
            return "Chưa có dữ liệu dinh dưỡng cho món này.";
        }
        return "Ước tính mỗi khẩu phần: "
                + formatNutritionValue(nutrition.getTotalCalories(), nutrition.getTotalCaloriesUnit(), "kcal")
                + ", protein " + formatNutritionValue(nutrition.getTotalProtein(), nutrition.getTotalProteinUnit(), "g")
                + ", carb " + formatNutritionValue(nutrition.getTotalCarbs(), nutrition.getTotalCarbsUnit(), "g")
                + ", fat " + formatNutritionValue(nutrition.getTotalFat(), nutrition.getTotalFatUnit(), "g")
                + ".";
    }

    private String formatNutritionValue(Double value, String unit, String fallbackUnit) {
        if (value == null) {
            return "chưa rõ";
        }
        String displayUnit = unit != null && !unit.isBlank() ? unit : fallbackUnit;
        return Math.round(value * 10.0) / 10.0 + " " + displayUnit;
    }

    private Double roundScore(Double score) {
        return Math.round(score * 100.0) / 100.0;
    }

    private FoodResponse mapToFoodResponse(FoodLibrary foodLibrary) {
        return mapToFoodResponse(foodLibrary, false);
    }

    private List<String> resolveAdvanceForValues(String advanceFor) {
        if (advanceFor == null || advanceFor.isBlank()) {
            return List.of();
        }
        return ADVANCE_FOR_ALIASES.getOrDefault(advanceFor, List.of(advanceFor));
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

    private FoodResponse mapToFoodListResponse(FoodLibrary foodLibrary) {
        return FoodResponse.builder()
                .id(foodLibrary.getId())
                .functionCode(foodLibrary.getFunctionCode())
                .name(foodLibrary.getName())
                .imageUrl(foodLibrary.getImageUrl())
                .advanceFor(foodLibrary.getAdvanceFor())
                .status(foodLibrary.getStatus())
                .nutrition(mapToNutritionResponse(foodLibrary.getNutritionSummary()))
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
                .food(foodLibrary != null ? mapToFoodListResponse(foodLibrary) : null)
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
                .food(foodLibrary != null ? mapToFoodListResponse(foodLibrary) : null)
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

    private static final class FoodSuggestionCandidate {
        private final FoodLibrary foodLibrary;
        private final LinkedHashSet<String> recipeIngredients = new LinkedHashSet<>();
        private final LinkedHashSet<String> matchedIngredients = new LinkedHashSet<>();
        private double score;

        private FoodSuggestionCandidate(FoodLibrary foodLibrary) {
            this.foodLibrary = foodLibrary;
        }

        private FoodLibrary foodLibrary() {
            return foodLibrary;
        }

        private LinkedHashSet<String> matchedIngredients() {
            return matchedIngredients;
        }

        private double score() {
            return score;
        }

        private void addRecipeIngredient(String ingredientName) {
            recipeIngredients.add(ingredientName);
        }

        private void addMatchedIngredient(String ingredientName) {
            matchedIngredients.add(ingredientName);
        }

        private List<String> missingIngredients() {
            return recipeIngredients.stream()
                    .filter(ingredient -> !matchedIngredients.contains(ingredient))
                    .collect(Collectors.toList());
        }

        private void calculateScore(String targetAdvanceFor) {
            double baseScore = recipeIngredients.isEmpty()
                    ? 0.0
                    : (double) matchedIngredients.size() / recipeIngredients.size();
            double ageBonus = 0.0;
            if (targetAdvanceFor != null
                    && FoodServiceImpl.ADVANCE_FOR_ALIASES.getOrDefault(targetAdvanceFor, List.of(targetAdvanceFor))
                    .contains(foodLibrary.getAdvanceFor())) {
                ageBonus = 0.25;
            }
            score = baseScore + ageBonus;
        }
    }
}
