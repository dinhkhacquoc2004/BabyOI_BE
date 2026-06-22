package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.IngredientAdjustmentSuggestRequest;
import com.example.babyoi_be.domain.dto.respone.IngredientAdjustmentSuggestionResponse;
import com.example.babyoi_be.domain.entity.FoodLibrary;
import com.example.babyoi_be.domain.entity.FoodLibraryIngredient;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.repository.FoodLibraryIngredientRepository;
import com.example.babyoi_be.repository.FoodLibraryRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.RestrictedFoodRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.NutritionAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NutritionAiServiceImpl implements NutritionAiService {

    private final ProfileRepository profileRepository;
    private final FoodLibraryRepository foodLibraryRepository;
    private final FoodLibraryIngredientRepository foodLibraryIngredientRepository;
    private final RestrictedFoodRepository restrictedFoodRepository;

    @Override
    public IngredientAdjustmentSuggestionResponse suggestIngredientAdjustments(IngredientAdjustmentSuggestRequest request) {
        Profile profile = validateCurrentProfile(request.getProfileId());
        FoodLibrary food = foodLibraryRepository.findById(request.getFoodId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn"));
        if (!Constants.TABLE_STATUS.ACTIVE.equals(food.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn");
        }
        if (restrictedFoodRepository.findFoodLibraryIdsByProfileId(profile.getId()).contains(food.getId())) {
            return IngredientAdjustmentSuggestionResponse.builder()
                    .status("NOT_RECOMMENDED")
                    .foodId(food.getId())
                    .profileId(profile.getId())
                    .storageKey(storageKey(profile.getId(), food.getId()))
                    .amounts(Map.of())
                    .summary("Món này đang nằm trong danh sách hạn chế của hồ sơ.")
                    .reason("BabyOi không gợi ý chỉnh khẩu phần cho món đã bị hạn chế.")
                    .warnings(List.of("Nếu có dị ứng hoặc phản ứng bất thường, nên tham khảo bác sĩ."))
                    .expiresInDays(1)
                    .build();
        }

        List<FoodLibraryIngredient> ingredients = foodLibraryIngredientRepository.findByFoodLibraryId(food.getId())
                .stream()
                .filter(ingredient -> !Constants.TABLE_STATUS.DELETED.equals(ingredient.getStatus()))
                .filter(ingredient -> ingredient.getAmountPerServing() != null && ingredient.getAmountPerServing() > 0)
                .toList();
        if (ingredients.isEmpty()) {
            return IngredientAdjustmentSuggestionResponse.builder()
                    .status("NEEDS_MORE_DATA")
                    .foodId(food.getId())
                    .profileId(profile.getId())
                    .storageKey(storageKey(profile.getId(), food.getId()))
                    .amounts(Map.of())
                    .summary("Món này chưa có dữ liệu nguyên liệu để cá nhân hóa.")
                    .reason("Cần danh sách nguyên liệu và định lượng gốc trước khi AI điều chỉnh.")
                    .warnings(List.of("Mẹ có thể xem khẩu phần gốc của món trong thư viện."))
                    .expiresInDays(1)
                    .build();
        }

        double factor = resolveAdjustmentFactor(request);
        Map<String, String> amounts = new LinkedHashMap<>();
        for (FoodLibraryIngredient ingredient : ingredients) {
            double baseAmount = ingredient.getAmountPerServing();
            double adjusted = clamp(baseAmount * factor, baseAmount * 0.5, baseAmount * 1.5);
            amounts.put(String.valueOf(ingredient.getId()), formatAmount(adjusted));
        }

        return IngredientAdjustmentSuggestionResponse.builder()
                .status("SUCCESS")
                .foodId(food.getId())
                .profileId(profile.getId())
                .storageKey(storageKey(profile.getId(), food.getId()))
                .amounts(amounts)
                .summary(factor >= 1.05 ? "Tăng nhẹ khẩu phần để hỗ trợ mục tiêu năng lượng." : factor <= 0.95 ? "Giảm nhẹ khẩu phần để dễ tiêu hơn." : "Giữ khẩu phần gần mức gốc của món.")
                .reason(buildReason(request, factor))
                .warnings(List.of("Định lượng chỉ là gợi ý tham khảo; nếu có dị ứng, sốt, nôn hoặc tiêu chảy, nên hỏi bác sĩ."))
                .expiresInDays(3)
                .build();
    }

    private Profile validateCurrentProfile(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof CustomUserDetails userDetails
                && (profile.getUser() == null || !userDetails.getId().equals(profile.getUser().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
        return profile;
    }

    private double resolveAdjustmentFactor(IngredientAdjustmentSuggestRequest request) {
        String text = ((request.getCurrentGoal() != null ? request.getCurrentGoal() : "") + " "
                + (request.getUserNotes() != null ? request.getUserNotes() : "")).toLowerCase(Locale.ROOT);
        if (containsAny(text, "tăng cân", "năng lượng", "lợi sữa", "ăn ngon")) {
            return 1.12;
        }
        if (containsAny(text, "dễ tiêu", "tiêu chảy", "nôn", "sốt", "mệt", "dị ứng", "không thích", "né")) {
            return 0.85;
        }
        return 1.0;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String buildReason(IngredientAdjustmentSuggestRequest request, double factor) {
        String notes = request.getUserNotes() != null ? request.getUserNotes().trim() : "";
        if (!notes.isBlank()) {
            return "BabyOi đã xét các yếu tố mẹ nhập: " + notes;
        }
        if (factor >= 1.05) {
            return "Mục tiêu hiện tại cần thêm năng lượng nên khẩu phần được tăng nhẹ trong giới hạn an toàn.";
        }
        if (factor <= 0.95) {
            return "Hồ sơ có yếu tố cần ăn nhẹ hơn nên khẩu phần được giảm vừa phải.";
        }
        return "Món đã khá phù hợp nên BabyOi giữ gần định lượng gốc.";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatAmount(double amount) {
        if (amount <= 0) {
            return "0";
        }
        BigDecimal value = BigDecimal.valueOf(amount).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        return value.toPlainString();
    }

    private String storageKey(Long profileId, Long foodId) {
        return "babyoi_food_detail_amounts:" + profileId + ":" + foodId;
    }
}
