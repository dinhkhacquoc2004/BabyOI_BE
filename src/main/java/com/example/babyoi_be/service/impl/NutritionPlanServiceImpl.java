package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.NutritionPlanGenerateRequest;
import com.example.babyoi_be.domain.dto.respone.FoodNutritionResponse;
import com.example.babyoi_be.domain.dto.respone.NutritionPlanDayResponse;
import com.example.babyoi_be.domain.dto.respone.NutritionPlanMealResponse;
import com.example.babyoi_be.domain.dto.respone.NutritionPlanResponse;
import com.example.babyoi_be.domain.entity.*;
import com.example.babyoi_be.repository.*;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.NutritionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionPlanServiceImpl implements NutritionPlanService {

    private static final String PROMPT_VERSION = "nutrition-plan-v1";
    private static final List<String> DEFAULT_MEAL_TYPES = List.of("BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER", "EVENING_SNACK");
    private static final Set<String> ALLOWED_MEAL_TYPES = Set.of(
            "BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER", "EVENING_SNACK"
    );
    private static final Map<String, String> COMPACT_MEAL_TYPES = Map.of(
            "B", "BREAKFAST",
            "MS", "MORNING_SNACK",
            "L", "LUNCH",
            "AS", "AFTERNOON_SNACK",
            "D", "DINNER",
            "ES", "EVENING_SNACK"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NutritionPlanRepository nutritionPlanRepository;
    private final ProfileRepository profileRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final IllnessEventRepository illnessEventRepository;
    private final FavoriteFoodRepository favoriteFoodRepository;
    private final RestrictedFoodRepository restrictedFoodRepository;
    private final FoodLibraryRepository foodLibraryRepository;

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${app.ai.gemini.text-model:gemini-3.5-flash,gemini-2.5-flash,gemini-2.5-flash-lite}")
    private String textModels;

    @Value("${app.ai.gemini.timeout-seconds:20}")
    private Long timeoutSeconds;

    @Override
    @Transactional
    public NutritionPlanResponse generatePlan(NutritionPlanGenerateRequest request) {
        LocalDate startDate = parseDate(request.getStartDate(), "startDate");
        LocalDate endDate = parseDate(request.getEndDate(), "endDate");
        validateDateRange(startDate, endDate);
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int mealsPerDay = resolveMealsPerDay(request.getMealsPerDay());
        List<String> mealTypes = resolveMealTypes(request.getAllowedMealTypes(), mealsPerDay);

        Profile profile = validateCurrentProfile(request.getProfileId());
        HealthRecord latestHealth = healthRecordRepository.findFirstByProfileIdOrderByRecordDateDesc(profile.getId()).orElse(null);
        List<HealthRecord> latestTwoHealth = healthRecordRepository.findTop2ByProfileIdOrderByRecordDateDesc(profile.getId());
        List<IllnessEvent> recentIllnesses = illnessEventRepository.findByProfileAndDateRange(
                profile.getId(),
                startDate.minusDays(30),
                endDate,
                null
        );
        List<Long> favoriteIds = favoriteFoodRepository.findFoodLibraryIdsByProfileId(profile.getId());
        List<Long> restrictedIds = restrictedFoodRepository.findFoodLibraryIdsByProfileId(profile.getId());
        List<FoodLibrary> candidateFoods = resolveCandidateFoods(request.getCandidateFoodIds(), restrictedIds);
        if (candidateFoods.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không có món ăn hợp lệ để lập lịch");
        }

        Map<String, Object> context = buildAiContext(
                profile,
                latestHealth,
                latestTwoHealth,
                recentIllnesses,
                startDate,
                endDate,
                days,
                mealsPerDay,
                mealTypes,
                request,
                favoriteIds,
                restrictedIds,
                candidateFoods
        );
        String requestHash = hashContext(context);
        Optional<NutritionPlan> cachedPlan = nutritionPlanRepository
                .findFirstByRequestHashAndStatusOrderByCreatedAtDesc(requestHash, Constants.TABLE_STATUS.ACTIVE);
        if (cachedPlan.isPresent()) {
            return mapPlan(cachedPlan.get(), true);
        }

        Optional<AiPlan> maybeAiPlan = callAiPlan(context);
        AiPlan aiPlan = maybeAiPlan
                .orElseGet(() -> buildRuleBasedPlan(startDate, days, mealTypes, request, candidateFoods, recentIllnesses, normalizedApiKey().isBlank()));
        try {
            validateAiPlan(aiPlan, startDate, endDate, days, mealsPerDay, mealTypes, candidateFoods, restrictedIds);
        } catch (ResponseStatusException exception) {
            if (maybeAiPlan.isEmpty()) {
                throw exception;
            }
            log.warn("Gemini nutrition plan output failed validation, using rule-based fallback. reason={}", exception.getReason());
            aiPlan = buildRuleBasedPlan(startDate, days, mealTypes, request, candidateFoods, recentIllnesses, false);
            validateAiPlan(aiPlan, startDate, endDate, days, mealsPerDay, mealTypes, candidateFoods, restrictedIds);
        }
        if (!"SUCCESS".equals(aiPlan.status())) {
            return NutritionPlanResponse.builder()
                    .status(aiPlan.status())
                    .profileId(profile.getId())
                    .profileName(profile.getName())
                    .startDate(startDate.toString())
                    .endDate(endDate.toString())
                    .mealsPerDay(mealsPerDay)
                    .summary(aiPlan.summary())
                    .warnings(aiPlan.warnings())
                    .aiModel(aiPlan.model())
                    .aiPromptVersion(PROMPT_VERSION)
                    .cached(false)
                    .days(List.of())
                    .build();
        }

        NutritionPlan plan = savePlan(profile, request, startDate, endDate, mealsPerDay, requestHash, aiPlan, candidateFoods);
        return mapPlan(plan, false);
    }

    private Optional<AiPlan> callAiPlan(Map<String, Object> context) {
        if (normalizedApiKey().isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> requestBody = buildGeminiRequestBody(context, null);
        for (String model : resolveTextModels()) {
            try {
                JsonNode response = WebClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader("x-goog-api-key", normalizedApiKey())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()
                        .post()
                        .uri("/{model}:generateContent", model)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(resolveTimeoutSeconds()))
                        .block();
                String outputText = extractOutputText(response);
                if (outputText.isBlank()) {
                    continue;
                }
                return Optional.of(parseAiPlan(outputText, model));
            } catch (WebClientResponseException exception) {
                log.warn("Gemini nutrition plan failed. status={}, model={}", exception.getStatusCode(), model);
            } catch (Exception exception) {
                log.warn("Gemini nutrition plan fallback used. model={}", model, exception);
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> buildGeminiRequestBody(Map<String, Object> context, String extraInstruction) {
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(context);
        } catch (Exception exception) {
            contextJson = context.toString();
        }
        String prompt = """
                Ban la AI Nutrition Planner cua BabyOi.
                Trả về JSON hợp lệ, không thêm text ngoài JSON.
                Chỉ chọn món từ foods, không tạo fid mới, không chọn món trong restrictedIds.
                Mỗi ngày phải có đúng req.meals bữa, số ngày đúng req.days, date nằm trong req.start và req.end.
                t chỉ được là một trong req.mealTypes.
                Nếu không thể tạo plan an toàn, trả status NEEDS_MORE_DATA và days rỗng.
                Ưu tiên né các món hoặc nhóm thực phẩm được nhắc trong req.notes.
                Output compact:
                {"status":"SUCCESS|NEEDS_MORE_DATA","summary":"string","warnings":["string"],"days":[{"date":"YYYY-MM-DD","i":1,"note":"string","meals":[{"t":"B|MS|L|AS|D|ES","fid":1,"p":"string","r":"string","w":null}]}]}

                Context:
                %s
                %s
                """.formatted(contextJson, extraInstruction == null ? "" : extraInstruction);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", prompt);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(textPart));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.25);
        generationConfig.put("maxOutputTokens", 4096);
        generationConfig.put("responseMimeType", "application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private AiPlan parseAiPlan(String outputText, String model) throws Exception {
        JsonNode root = objectMapper.readTree(cleanJsonText(outputText));
        String status = root.path("status").asText("NEEDS_MORE_DATA");
        String summary = compactText(root.path("summary").asText(""), 500);
        List<String> warnings = readTextList(root.get("warnings"), 500);
        List<AiDay> days = new ArrayList<>();
        JsonNode daysNode = root.get("days");
        if (daysNode != null && daysNode.isArray()) {
            for (JsonNode dayNode : daysNode) {
                List<AiMeal> meals = new ArrayList<>();
                JsonNode mealsNode = dayNode.get("meals");
                if (mealsNode != null && mealsNode.isArray()) {
                    for (JsonNode mealNode : mealsNode) {
                        String rawType = firstText(mealNode, "t", "mealType");
                        meals.add(new AiMeal(
                                normalizeMealType(rawType),
                                firstLong(mealNode, "fid", "foodId"),
                                compactText(firstText(mealNode, "p", "portion"), 120),
                                compactText(firstText(mealNode, "r", "reason"), 500),
                                nullableCompactText(firstText(mealNode, "w", "warning"), 500)
                        ));
                    }
                }
                days.add(new AiDay(
                        LocalDate.parse(firstText(dayNode, "date", "planDate")),
                        firstInt(dayNode, "i", "dayIndex"),
                        compactText(firstText(dayNode, "note"), 500),
                        meals
                ));
            }
        }
        return new AiPlan(status, summary, warnings, days, model);
    }

    private AiPlan buildRuleBasedPlan(
            LocalDate startDate,
            int days,
            List<String> mealTypes,
            NutritionPlanGenerateRequest request,
            List<FoodLibrary> candidateFoods,
            List<IllnessEvent> recentIllnesses,
            boolean apiKeyMissing
    ) {
        List<String> warnings = new ArrayList<>();
        if (apiKeyMissing) {
            warnings.add("AI Gemini chưa được cấu hình, BabyOi tạo lịch theo quy tắc tạm thời.");
        } else {
            warnings.add("AI chưa trả về JSON hợp lệ, BabyOi tạo lịch theo quy tắc tạm thời.");
        }
        if (!recentIllnesses.isEmpty()) {
            warnings.add("Có lịch sử bệnh gần đây, nên theo dõi ăn uống và hỏi bác sĩ nếu triệu chứng nặng.");
        }
        List<AiDay> planDays = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < days; dayIndex++) {
            List<AiMeal> meals = new ArrayList<>();
            for (int mealIndex = 0; mealIndex < mealTypes.size(); mealIndex++) {
                FoodLibrary food = candidateFoods.get((dayIndex + mealIndex) % candidateFoods.size());
                meals.add(new AiMeal(
                        mealTypes.get(mealIndex),
                        food.getId(),
                        defaultPortion(mealTypes.get(mealIndex)),
                        "Món trong thư viện BabyOi, phù hợp để xoay vòng bữa ăn.",
                        null
                ));
            }
            planDays.add(new AiDay(
                    startDate.plusDays(dayIndex),
                    dayIndex + 1,
                    "Theo doi phan ung va dieu chinh neu can.",
                    meals
            ));
        }
        String summary = request.getCurrentGoal() != null && !request.getCurrentGoal().isBlank()
                ? "Kế hoạch hỗ trợ mục tiêu: " + compactText(request.getCurrentGoal(), 80)
                : "Kế hoạch ăn BabyOi theo món có sẵn trong thư viện.";
        return new AiPlan("SUCCESS", summary, warnings, planDays, apiKeyMissing ? "RULE_BASED_NO_AI_KEY" : "RULE_BASED_AI_FALLBACK");
    }

    private void validateAiPlan(
            AiPlan aiPlan,
            LocalDate startDate,
            LocalDate endDate,
            int expectedDays,
            int mealsPerDay,
            List<String> mealTypes,
            List<FoodLibrary> candidateFoods,
            List<Long> restrictedIds
    ) {
        if (!Set.of("SUCCESS", "NEEDS_MORE_DATA").contains(aiPlan.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả status không hợp lệ");
        }
        if (!"SUCCESS".equals(aiPlan.status())) {
            return;
        }
        Set<Long> candidateIds = candidateFoods.stream().map(FoodLibrary::getId).collect(Collectors.toSet());
        Set<Long> restrictedSet = new HashSet<>(restrictedIds);
        if (aiPlan.days().size() != expectedDays) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả sai số ngày trong lịch ăn");
        }
        Set<LocalDate> seenDates = new HashSet<>();
        for (AiDay day : aiPlan.days()) {
            if (day.date().isBefore(startDate) || day.date().isAfter(endDate) || !seenDates.add(day.date())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả ngày không hợp lệ");
            }
            if (day.dayIndex() == null || day.dayIndex() < 1 || day.dayIndex() > expectedDays) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả dayIndex không hợp lệ");
            }
            if (day.meals().size() != mealsPerDay) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả sai số bữa mỗi ngày");
            }
            for (AiMeal meal : day.meals()) {
                if (!mealTypes.contains(meal.mealType())) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI trả mealType không hợp lệ");
                }
                if (meal.foodId() == null || !candidateIds.contains(meal.foodId()) || restrictedSet.contains(meal.foodId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI chọn món ăn không hợp lệ");
                }
            }
        }
    }

    private NutritionPlan savePlan(
            Profile profile,
            NutritionPlanGenerateRequest request,
            LocalDate startDate,
            LocalDate endDate,
            int mealsPerDay,
            String requestHash,
            AiPlan aiPlan,
            List<FoodLibrary> candidateFoods
    ) {
        Map<Long, FoodLibrary> foodsById = candidateFoods.stream()
                .collect(Collectors.toMap(FoodLibrary::getId, food -> food));
        LocalDateTime now = LocalDateTime.now();
        NutritionPlan plan = NutritionPlan.builder()
                .profile(profile)
                .currentGoal(compactText(request.getCurrentGoal(), 2000))
                .futureGoal(compactText(resolveUserNotes(request), 2000))
                .startDate(startDate)
                .endDate(endDate)
                .mealsPerDay(mealsPerDay)
                .summary(compactText(aiPlan.summary(), 2000))
                .warningsJson(toJson(aiPlan.warnings()))
                .status(Constants.TABLE_STATUS.ACTIVE)
                .aiModel(aiPlan.model())
                .aiPromptVersion(PROMPT_VERSION)
                .requestHash(requestHash)
                .createdAt(now)
                .updatedAt(now)
                .build();
        List<NutritionPlanDay> days = aiPlan.days().stream()
                .map(day -> {
                    NutritionPlanDay planDay = NutritionPlanDay.builder()
                            .plan(plan)
                            .planDate(day.date())
                            .dayIndex(day.dayIndex())
                            .note(day.note())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    List<NutritionPlanMeal> meals = new ArrayList<>(day.meals().stream()
                            .map(meal -> {
                                FoodLibrary food = foodsById.get(meal.foodId());
                                return NutritionPlanMeal.builder()
                                        .planDay(planDay)
                                        .mealType(meal.mealType())
                                        .foodLibrary(food)
                                        .foodNameSnapshot(food.getName())
                                        .portion(meal.portion())
                                        .reason(meal.reason())
                                        .warning(meal.warning())
                                        .eatenStatus("PLANNED")
                                        .createdAt(now)
                                        .updatedAt(now)
                                        .build();
                            })
                            .toList());
                    planDay.setMeals(meals);
                    return planDay;
                })
                .collect(Collectors.toCollection(ArrayList::new));
        plan.setDays(days);
        return nutritionPlanRepository.save(plan);
    }

    private Map<String, Object> buildAiContext(
            Profile profile,
            HealthRecord latestHealth,
            List<HealthRecord> latestTwoHealth,
            List<IllnessEvent> recentIllnesses,
            LocalDate startDate,
            LocalDate endDate,
            int days,
            int mealsPerDay,
            List<String> mealTypes,
            NutritionPlanGenerateRequest request,
            List<Long> favoriteIds,
            List<Long> restrictedIds,
            List<FoodLibrary> candidateFoods
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> profileContext = new LinkedHashMap<>();
        profileContext.put("id", profile.getId());
        profileContext.put("name", defaultString(profile.getName()));
        profileContext.put("type", isMotherProfile(profile) ? "M" : "B");
        profileContext.put("ageM", resolveAgeMonths(profile));
        profileContext.put("sex", profile.getSex() != null ? profile.getSex().name() : "UNKNOWN");
        context.put("p", profileContext);
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("src", latestHealth != null ? "EXISTING_HEALTH" : "NO_HEALTH");
        health.put("height", latestHealth != null ? latestHealth.getHeight() : null);
        health.put("weight", latestHealth != null ? latestHealth.getWeight() : null);
        health.put("bmi", latestHealth != null ? latestHealth.getBmi() : null);
        health.put("tdee", latestHealth != null ? latestHealth.getTdee() : null);
        health.put("trend", resolveWeightTrend(latestTwoHealth));
        context.put("h", health);
        context.put("ill", recentIllnesses.stream().limit(5).map(event -> Map.of(
                "t", defaultString(event.getIllnessType()),
                "sev", event.getStatus() != null ? event.getStatus() : 1L,
                "start", event.getStartAt() != null ? event.getStartAt().toString() : "",
                "end", event.getEndAt() != null ? event.getEndAt().toString() : ""
        )).toList());
        context.put("req", Map.of(
                "start", startDate.toString(),
                "end", endDate.toString(),
                "days", days,
                "meals", mealsPerDay,
                "mealTypes", mealTypes.stream().map(this::toCompactMealType).toList(),
                "goal", defaultString(request.getCurrentGoal()),
                "notes", defaultString(resolveUserNotes(request))
        ));
        context.put("favoriteIds", favoriteIds);
        context.put("restrictedIds", restrictedIds);
        context.put("foods", candidateFoods.stream().limit(50).map(this::compactFood).toList());
        return context;
    }

    private List<FoodLibrary> resolveCandidateFoods(List<Long> requestedIds, List<Long> restrictedIds) {
        Set<Long> restrictedSet = new HashSet<>(restrictedIds);
        List<FoodLibrary> foods;
        if (requestedIds != null && !requestedIds.isEmpty()) {
            List<Long> cleanIds = requestedIds.stream().filter(Objects::nonNull).distinct().limit(50).toList();
            foods = foodLibraryRepository.findByIdInAndStatus(cleanIds, Constants.TABLE_STATUS.ACTIVE);
        } else {
            foods = foodLibraryRepository.findTop50ByStatusOrderByIdAsc(Constants.TABLE_STATUS.ACTIVE);
        }
        return foods.stream()
                .filter(food -> !restrictedSet.contains(food.getId()))
                .limit(50)
                .toList();
    }

    private NutritionPlanResponse mapPlan(NutritionPlan plan, boolean cached) {
        List<NutritionPlanDayResponse> days = plan.getDays() == null ? List.of() : plan.getDays().stream()
                .sorted(Comparator.comparing(NutritionPlanDay::getPlanDate).thenComparing(NutritionPlanDay::getDayIndex))
                .map(day -> NutritionPlanDayResponse.builder()
                        .id(day.getId())
                        .date(day.getPlanDate().toString())
                        .dayIndex(day.getDayIndex())
                        .note(day.getNote())
                        .meals(day.getMeals() == null ? List.of() : day.getMeals().stream()
                                .map(this::mapMeal)
                                .toList())
                        .build())
                .toList();
        return NutritionPlanResponse.builder()
                .status("SUCCESS")
                .planId(plan.getId())
                .profileId(plan.getProfile().getId())
                .profileName(plan.getProfile().getName())
                .startDate(plan.getStartDate().toString())
                .endDate(plan.getEndDate().toString())
                .mealsPerDay(plan.getMealsPerDay())
                .summary(plan.getSummary())
                .warnings(parseWarnings(plan.getWarningsJson()))
                .aiModel(plan.getAiModel())
                .aiPromptVersion(plan.getAiPromptVersion())
                .cached(cached)
                .days(days)
                .build();
    }

    private NutritionPlanMealResponse mapMeal(NutritionPlanMeal meal) {
        FoodLibrary food = meal.getFoodLibrary();
        return NutritionPlanMealResponse.builder()
                .id(meal.getId())
                .mealType(meal.getMealType())
                .foodId(food != null ? food.getId() : null)
                .foodName(food != null ? food.getName() : meal.getFoodNameSnapshot())
                .foodImageUrl(food != null ? food.getImageUrl() : null)
                .nutrition(food != null ? mapNutrition(food.getNutritionSummary()) : null)
                .portion(meal.getPortion())
                .reason(meal.getReason())
                .warning(meal.getWarning())
                .eatenStatus(meal.getEatenStatus())
                .build();
    }

    private FoodNutritionResponse mapNutrition(FoodNutritionSummary nutrition) {
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

    private Map<String, Object> compactFood(FoodLibrary food) {
        FoodNutritionSummary nutrition = food.getNutritionSummary();
        List<String> tags = new ArrayList<>();
        FoodRecommendation recommendation = food.getRecommendation();
        if (recommendation != null) {
            if (recommendation.getGoodPoints() != null && !recommendation.getGoodPoints().isBlank()) {
                tags.add(compactText(recommendation.getGoodPoints(), 80));
            }
            if (recommendation.getBadPoints() != null && !recommendation.getBadPoints().isBlank()) {
                tags.add(compactText(recommendation.getBadPoints(), 80));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", food.getId());
        result.put("n", food.getName());
        result.put("age", food.getAdvanceFor());
        result.put("fn", food.getFunctionCode());
        result.put("cal", nutrition != null ? nutrition.getTotalCalories() : null);
        result.put("pro", nutrition != null ? nutrition.getTotalProtein() : null);
        result.put("tags", tags);
        return result;
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must use yyyy-MM-dd");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể tạo tối đa 7 ngày mỗi lần");
        }
    }

    private int resolveMealsPerDay(Integer mealsPerDay) {
        int value = mealsPerDay != null ? mealsPerDay : 4;
        if (value < 3 || value > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mealsPerDay phải từ 3 đến 6");
        }
        return value;
    }

    private List<String> resolveMealTypes(List<String> requestedMealTypes, int mealsPerDay) {
        List<String> mealTypes = requestedMealTypes == null || requestedMealTypes.isEmpty()
                ? DEFAULT_MEAL_TYPES.subList(0, mealsPerDay)
                : requestedMealTypes.stream()
                .map(this::normalizeMealType)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (mealTypes.size() != mealsPerDay) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "allowedMealTypes phải có đúng mealsPerDay giá trị");
        }
        if (!ALLOWED_MEAL_TYPES.containsAll(mealTypes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "allowedMealTypes có giá trị không hợp lệ");
        }
        return mealTypes;
    }

    private Integer resolveAgeMonths(Profile profile) {
        if (profile.getDateOfBirth() == null || profile.getDateOfBirth().isAfter(LocalDate.now())) {
            return null;
        }
        Period age = Period.between(profile.getDateOfBirth(), LocalDate.now());
        return age.getYears() * 12 + age.getMonths();
    }

    private String resolveWeightTrend(List<HealthRecord> records) {
        if (records == null || records.size() < 2 || records.get(0).getWeight() == null || records.get(1).getWeight() == null) {
            return "UNKNOWN";
        }
        double diff = records.get(0).getWeight() - records.get(1).getWeight();
        if (diff > 0.3) {
            return "GAIN";
        }
        if (diff < -0.2) {
            return "WEIGHT_LOSS";
        }
        return "STABLE";
    }

    private boolean isMotherProfile(Profile profile) {
        String type = profile.getProfileType() != null ? profile.getProfileType().trim().toUpperCase(Locale.ROOT) : "";
        return "MOTHER".equals(type) || "MOM".equals(type);
    }

    private String resolveUserNotes(NutritionPlanGenerateRequest request) {
        if (request.getUserNotes() != null && !request.getUserNotes().isBlank()) {
            return request.getUserNotes();
        }
        return request.getFutureGoal();
    }

    private String normalizeMealType(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return COMPACT_MEAL_TYPES.getOrDefault(normalized, normalized);
    }

    private String toCompactMealType(String mealType) {
        return COMPACT_MEAL_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(mealType))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(mealType);
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return "";
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText("");
            }
        }
        return "";
    }

    private Long firstLong(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer firstInt(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<String> readTextList(JsonNode node, int maxLength) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = compactText(item.asText(""), maxLength);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        JsonNode candidates = response.get("candidates");
        if (candidates != null && candidates.isArray()) {
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                if (parts != null && parts.isArray()) {
                    for (JsonNode part : parts) {
                        JsonNode text = part.get("text");
                        if (text != null && text.isTextual()) {
                            builder.append(text.asText());
                        }
                    }
                }
            }
        }
        return builder.toString().trim();
    }

    private String cleanJsonText(String outputText) {
        String text = outputText == null ? "" : outputText.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }

    private String normalizedApiKey() {
        return apiKey != null ? apiKey.replaceAll("\\s+", "") : "";
    }

    private List<String> resolveTextModels() {
        if (textModels == null || textModels.isBlank()) {
            return List.of("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite");
        }
        return List.of(textModels.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private long resolveTimeoutSeconds() {
        return timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 20L;
    }

    private String compactText(String text, int maxLength) {
        String compact = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxLength ? compact : compact.substring(0, maxLength);
    }

    private String nullableCompactText(String text, int maxLength) {
        String value = compactText(text, maxLength);
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String defaultPortion(String mealType) {
        return switch (mealType) {
            case "MORNING_SNACK", "AFTERNOON_SNACK", "EVENING_SNACK" -> "1 phan nho";
            default -> "1 khau phan vua";
        };
    }

    private String hashContext(Map<String, Object> context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append("%02x".formatted(value));
            }
            return builder.toString();
        } catch (Exception exception) {
            return UUID.nameUUIDFromBytes(context.toString().getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private List<String> parseWarnings(String warningsJson) {
        try {
            return readTextList(objectMapper.readTree(warningsJson == null ? "[]" : warningsJson), 500);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private record AiPlan(String status, String summary, List<String> warnings, List<AiDay> days, String model) {
    }

    private record AiDay(LocalDate date, Integer dayIndex, String note, List<AiMeal> meals) {
    }

    private record AiMeal(String mealType, Long foodId, String portion, String reason, String warning) {
    }
}
