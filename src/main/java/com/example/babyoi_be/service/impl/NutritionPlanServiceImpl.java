package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.NutritionPlanGenerateRequest;
import com.example.babyoi_be.domain.dto.request.NutritionPlanMealUpdateRequest;
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

import java.text.Normalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final String PROMPT_VERSION = "nutrition-plan-v8-child-auto-meals";
    private static final int MIN_COMPLEMENTARY_FEEDING_AGE_MONTHS = 6;
    private static final double WHO_COMPLEMENTARY_KCAL_6_TO_8_MONTHS = 200D;
    private static final double WHO_COMPLEMENTARY_KCAL_9_TO_11_MONTHS = 300D;
    private static final double WHO_COMPLEMENTARY_KCAL_12_TO_23_MONTHS = 550D;
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
    private final FoodLibraryIngredientRepository foodLibraryIngredientRepository;
    private final NutritionPlanMealRepository nutritionPlanMealRepository;

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${app.ai.gemini.text-model:gemini-3.5-flash,gemini-2.5-flash,gemini-2.5-flash-lite}")
    private String textModels;

    @Value("${app.ai.gemini.timeout-seconds:40}")
    private Long timeoutSeconds;

    @Override
    @Transactional
    public NutritionPlanResponse generatePlan(NutritionPlanGenerateRequest request) {
        LocalDate startDate = parseDate(request.getStartDate(), "startDate");
        LocalDate endDate = parseDate(request.getEndDate(), "endDate");
        validateDateRange(startDate, endDate);
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        Profile profile = validateCurrentProfile(request.getProfileId());
        profileRepository.lockById(profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        validateBabyPlanningAge(profile);
        int mealsPerDay = resolveMealsPerDay(request.getMealsPerDay(), profile);
        List<String> mealTypes = resolveMealTypes(
                isMotherProfile(profile) ? request.getAllowedMealTypes() : null,
                mealsPerDay
        );
        validateGoalTarget(profile, request);
        if (nutritionPlanRepository.existsByProfileIdAndStatus(profile.getId(), Constants.TABLE_STATUS.ACTIVE)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Hồ sơ đang có một lịch ăn chưa hoàn thành. Hãy hoàn thành hoặc xóa lịch đó trước khi tạo lịch mới"
            );
        }
        HealthRecord latestHealth = healthRecordRepository.findFirstByProfileIdOrderByRecordDateDescIdDesc(profile.getId()).orElse(null);
        validateTdeeAvailable(latestHealth, profile);
        List<HealthRecord> latestTwoHealth = healthRecordRepository.findTop2ByProfileIdOrderByRecordDateDescIdDesc(profile.getId());
        List<IllnessEvent> recentIllnesses = illnessEventRepository.findByProfileAndDateRange(
                profile.getId(),
                startDate.minusDays(30),
                endDate,
                null
        );
        List<Long> favoriteIds = favoriteFoodRepository.findFoodLibraryIdsByProfileId(profile.getId());
        List<Long> restrictedIds = restrictedFoodRepository.findFoodLibraryIdsByProfileId(profile.getId());
        List<String> targetAdvanceFor = resolveTargetAdvanceFor(profile, request);
        List<FoodLibrary> candidateFoods = resolveCandidateFoods(profile, request, targetAdvanceFor, restrictedIds);
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
                targetAdvanceFor,
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
        aiPlan = appendComplementaryFeedingWarning(aiPlan, profile);
        if (!"SUCCESS".equals(aiPlan.status())) {
            return NutritionPlanResponse.builder()
                    .status(aiPlan.status())
                    .profileId(profile.getId())
                    .profileName(profile.getName())
                    .profileType(profile.getProfileType())
                    .currentGoal(resolveCurrentGoal(profile, request))
                    .goalCode(resolveGoalCode(profile, request))
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

    @Override
    @Transactional(readOnly = true)
    public List<NutritionPlanResponse> getPlanHistory(Long profileId) {
        Profile profile = validateCurrentProfile(profileId);
        return nutritionPlanRepository.findByProfileIdAndStatusInOrderByCreatedAtDescIdDesc(
                        profile.getId(),
                        List.of(Constants.TABLE_STATUS.ACTIVE, Constants.TABLE_STATUS.SUCCESS)
                )
                .stream()
                .map(plan -> mapPlan(plan, false))
                .toList();
    }

    @Override
    @Transactional
    public NutritionPlanResponse updateMeal(Long mealId, NutritionPlanMealUpdateRequest request) {
        NutritionPlanMeal meal = nutritionPlanMealRepository.findById(mealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bữa ăn trong lịch"));
        NutritionPlan plan = meal.getPlanDay().getPlan();
        validatePlanEditable(plan);
        validateCurrentUser(plan.getProfile().getUser().getId());

        if (request.getMealType() != null && !request.getMealType().isBlank()) {
            String mealType = normalizeMealType(request.getMealType());
            if (!ALLOWED_MEAL_TYPES.contains(mealType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mealType không hợp lệ");
            }
            meal.setMealType(mealType);
        }
        if (request.getFoodId() != null) {
            FoodLibrary food = foodLibraryRepository.findById(request.getFoodId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn"));
            if (!Constants.TABLE_STATUS.ACTIVE.equals(food.getStatus())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn");
            }
            if (!isFoodForProfile(food, plan.getProfile())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Món ăn không phù hợp với đối tượng của hồ sơ");
            }
            if (plan.getGoalCode() != null
                    && !plan.getGoalCode().isBlank()
                    && !expandGoalCode(plan.getGoalCode().toUpperCase(Locale.ROOT)).contains(food.getAdvanceFor())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Món ăn không đúng mục tiêu của lịch ăn");
            }
            if (restrictedFoodRepository.findFoodLibraryIdsByProfileId(plan.getProfile().getId()).contains(food.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Món này đang nằm trong danh sách hạn chế của hồ sơ");
            }
            meal.setFoodLibrary(food);
            meal.setFoodNameSnapshot(food.getName());
            meal.setReason("Đã thay món theo mong muốn của người dùng.");
            meal.setWarning(null);
        }
        if (request.getPortion() != null) {
            meal.setPortion(nullableCompactText(request.getPortion(), 255));
        }
        if (request.getReason() != null) {
            meal.setReason(nullableCompactText(request.getReason(), 1000));
        }
        if (request.getWarning() != null) {
            meal.setWarning(nullableCompactText(request.getWarning(), 1000));
        }
        if (request.getEatenStatus() != null && !request.getEatenStatus().isBlank()) {
            meal.setEatenStatus(compactText(request.getEatenStatus(), 30).toUpperCase(Locale.ROOT));
        }
        LocalDateTime now = LocalDateTime.now();
        meal.setUpdatedAt(now);
        plan.setUpdatedAt(now);

        nutritionPlanMealRepository.save(meal);
        return mapPlan(plan, false);
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        NutritionPlan plan = nutritionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch ăn"));
        validatePlanDeletable(plan);
        validateCurrentUser(plan.getProfile().getUser().getId());
        plan.setStatus(Constants.TABLE_STATUS.DELETED);
        plan.setUpdatedAt(LocalDateTime.now());
        nutritionPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public NutritionPlanResponse completePlan(Long planId) {
        NutritionPlan plan = nutritionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch ăn"));
        validatePlanEditable(plan);
        validateCurrentUser(plan.getProfile().getUser().getId());
        plan.setStatus(Constants.TABLE_STATUS.SUCCESS);
        plan.setUpdatedAt(LocalDateTime.now());
        return mapPlan(nutritionPlanRepository.save(plan), false);
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
                    log.warn("Gemini nutrition plan returned no text. model={}, finishReason={}",
                            model, extractFinishReason(response));
                    continue;
                }
                try {
                    return Optional.of(parseAiPlan(outputText, model));
                } catch (Exception parseException) {
                    log.warn(
                            "Gemini nutrition plan returned malformed JSON. model={}, finishReason={}, outputChars={}, error={}",
                            model,
                            extractFinishReason(response),
                            outputText.length(),
                            compactText(rootCause(parseException).getMessage(), 220)
                    );
                    log.debug("Malformed Gemini nutrition plan output. model={}, output={}",
                            model, compactText(outputText, 4000));
                }
            } catch (WebClientResponseException exception) {
                log.warn("Gemini nutrition plan failed. model={}, status={}, body={}",
                        model,
                        exception.getStatusCode(),
                        compactText(exception.getResponseBodyAsString(), 300));
            } catch (Exception exception) {
                Throwable cause = rootCause(exception);
                log.warn("Gemini nutrition plan failed. model={}, error={}, message={}",
                        model,
                        cause.getClass().getSimpleName(),
                        compactText(cause.getMessage(), 300));
                log.debug("Gemini nutrition plan failure details. model={}", model, exception);
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
                Tu dong can chinh p/khau phan theo h.tdee, h.weight, h.bmi, h.trend, req.goal va req.targetWeightKg; khong de nguoi dung bam nut chinh luong rieng.
                Voi me: tong nang luong moi ngay khong vuot h.tdee; muc tieu giam can, de tieu hoac giu dang co the thap hon TDEE.
                Voi be: p.complementaryKcal la muc nang luong toi da tu thuc an bo sung moi ngay, khong phai toan bo TDEE/EER; sua van la nguon dinh duong chinh.
                Voi be: tong kcal cua cac bua khong vuot p.complementaryKcal va khong tang khau phan de bu toan bo h.tdee.
                Voi be 6-11 thang: sua me van la nguon dinh duong chinh, cac bua an dam chi la phan bo sung.
                Giu output ngan: summary toi da 20 tu; moi warning 15 tu; note 10 tu; p 6 tu; r 10 tu; w 10 tu.
                w phai la chuoi, dung chuoi rong neu khong co canh bao.
                Output compact:
                {"status":"SUCCESS|NEEDS_MORE_DATA","summary":"string","warnings":["string"],"days":[{"date":"YYYY-MM-DD","i":1,"note":"string","meals":[{"t":"B|MS|L|AS|D|ES","fid":1,"p":"string","r":"string","w":""}]}]}

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
        generationConfig.put("temperature", 0.15);
        generationConfig.put("maxOutputTokens", 8192);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", nutritionPlanResponseSchema());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private Map<String, Object> nutritionPlanResponseSchema() {
        Map<String, Object> mealSchema = schemaObject(
                Map.of(
                        "t", schemaString(List.of("B", "MS", "L", "AS", "D", "ES")),
                        "fid", Map.of("type", "INTEGER"),
                        "p", schemaString(),
                        "r", schemaString(),
                        "w", schemaString()
                ),
                List.of("t", "fid", "p", "r", "w")
        );
        Map<String, Object> daySchema = schemaObject(
                Map.of(
                        "date", schemaString(),
                        "i", Map.of("type", "INTEGER"),
                        "note", schemaString(),
                        "meals", Map.of("type", "ARRAY", "items", mealSchema)
                ),
                List.of("date", "i", "note", "meals")
        );
        return schemaObject(
                Map.of(
                        "status", schemaString(List.of("SUCCESS", "NEEDS_MORE_DATA")),
                        "summary", schemaString(),
                        "warnings", Map.of("type", "ARRAY", "items", schemaString()),
                        "days", Map.of("type", "ARRAY", "items", daySchema)
                ),
                List.of("status", "summary", "warnings", "days")
        );
    }

    private Map<String, Object> schemaObject(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private Map<String, Object> schemaString() {
        return Map.of("type", "STRING");
    }

    private Map<String, Object> schemaString(List<String> values) {
        return Map.of("type", "STRING", "enum", values);
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
        String resolvedGoal = resolveCurrentGoal(null, request);
        String summary = resolvedGoal != null && !resolvedGoal.isBlank()
                ? "Kế hoạch hỗ trợ mục tiêu: " + compactText(resolvedGoal, 80)
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
                .currentGoal(compactText(resolveCurrentGoal(profile, request), 2000))
                .goalCode(compactText(resolveGoalCode(profile, request), 80))
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
            List<String> targetAdvanceFor,
            List<Long> favoriteIds,
            List<Long> restrictedIds,
            List<FoodLibrary> candidateFoods
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("pv", PROMPT_VERSION);
        Map<String, Object> profileContext = new LinkedHashMap<>();
        profileContext.put("id", profile.getId());
        profileContext.put("name", defaultString(profile.getName()));
        profileContext.put("type", isMotherProfile(profile) ? "M" : "B");
        profileContext.put("ageM", resolveAgeMonths(profile));
        profileContext.put("feedingStage", resolveFeedingStage(profile));
        profileContext.put("complementaryKcal", resolveComplementaryFoodEnergyTarget(profile));
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
        Map<String, Object> requestContext = new LinkedHashMap<>();
        requestContext.put("start", startDate.toString());
        requestContext.put("end", endDate.toString());
        requestContext.put("days", days);
        requestContext.put("meals", mealsPerDay);
        requestContext.put("mealTypes", mealTypes.stream().map(this::toCompactMealType).toList());
        requestContext.put("goal", resolveCurrentGoal(profile, request));
        requestContext.put("goalCode", resolveGoalCode(profile, request));
        requestContext.put("targetWeightKg", request.getTargetWeightKg());
        requestContext.put("advanceFor", targetAdvanceFor);
        requestContext.put("notes", defaultString(resolveUserNotes(request)));
        context.put("req", requestContext);
        context.put("favoriteIds", favoriteIds);
        context.put("restrictedIds", restrictedIds);
        context.put("foods", candidateFoods.stream().limit(50).map(this::compactFood).toList());
        return context;
    }

    private List<FoodLibrary> resolveCandidateFoods(
            Profile profile,
            NutritionPlanGenerateRequest request,
            List<String> targetAdvanceFor,
            List<Long> restrictedIds) {
        Set<Long> restrictedSet = new HashSet<>(restrictedIds);
        List<Long> requestedIds = request.getCandidateFoodIds();
        boolean hasRequestedIds = requestedIds != null && !requestedIds.isEmpty();
        List<FoodLibrary> foods = List.of();
        if (hasRequestedIds) {
            List<Long> cleanIds = requestedIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(50)
                    .toList();
            foods = foodLibraryRepository.findByIdInAndStatus(cleanIds, Constants.TABLE_STATUS.ACTIVE);
        }

        if (foods.isEmpty() && !targetAdvanceFor.isEmpty()) {
            foods = foodLibraryRepository.findByStatusAndAdvanceForInOrderByIdAsc(Constants.TABLE_STATUS.ACTIVE, targetAdvanceFor);
        }

        if (foods.isEmpty()) {
            foods = foodLibraryRepository.findTop50ByStatusOrderByIdAsc(Constants.TABLE_STATUS.ACTIVE)
                    .stream()
                    .filter(food -> isFoodForProfile(food, profile))
                    .toList();
        }

        return foods.stream()
                .filter(food -> isFoodForProfile(food, profile))
                .filter(food -> targetAdvanceFor.isEmpty() || targetAdvanceFor.contains(food.getAdvanceFor()))
                .filter(food -> !restrictedSet.contains(food.getId()))
                .limit(50)
                .toList();
    }

    private List<String> resolveTargetAdvanceFor(Profile profile, NutritionPlanGenerateRequest request) {
        String goalCode = resolveGoalCode(profile, request);
        if (!goalCode.isBlank()) {
            return expandGoalCode(goalCode);
        }
        if (!isMotherProfile(profile)) {
            return babyAdvanceForByAge(resolveAgeMonths(profile));
        }

        String goal = request.getCurrentGoal();
        if (isWeightLossGoal(goal) || containsNormalized(goal, "giu dang")) {
            return List.of(Constants.FOOD_ADVICE_FOR.MOM_GET_BACK_IN_SHAPE);
        }
        if (containsNormalized(goal, "loi sua") || containsNormalized(goal, "sua")) {
            return List.of(
                    Constants.FOOD_ADVICE_FOR.MOM_INCREASE_MILK_SUPPLY,
                    Constants.FOOD_ADVICE_FOR.MOM_POSTPARTUM_BREASTFEEDING
            );
        }
        if (containsNormalized(goal, "tieu hoa")) {
            return List.of(Constants.FOOD_ADVICE_FOR.MOM_DIGESTION_RECOVERY);
        }
        if (containsNormalized(goal, "nang luong") || containsNormalized(goal, "tang can")) {
            return List.of(Constants.FOOD_ADVICE_FOR.MOM_HEALTHY_ENERGY);
        }
        if (containsNormalized(goal, "an ngon")) {
            return List.of(Constants.FOOD_ADVICE_FOR.MOM_CHANGE_DIET);
        }
        return motherAdvanceForValues();
    }

    private String resolveGoalCode(Profile profile, NutritionPlanGenerateRequest request) {
        if (!isMotherProfile(profile)) {
            return babyAdvanceForByAge(resolveAgeMonths(profile)).get(0);
        }
        String requestedCode = request.getGoalCode() == null
                ? ""
                : request.getGoalCode().trim().toUpperCase(Locale.ROOT);
        if (requestedCode.isBlank()) {
            return "";
        }
        List<String> allowedCodes = motherAdvanceForValues();
        if (!allowedCodes.contains(requestedCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mục tiêu không phù hợp với đối tượng hoặc độ tuổi của hồ sơ"
            );
        }
        return requestedCode;
    }

    private String resolveCurrentGoal(Profile profile, NutritionPlanGenerateRequest request) {
        if (profile != null && !isMotherProfile(profile)) {
            return "Dinh dưỡng phù hợp độ tuổi";
        }
        return defaultString(request.getCurrentGoal());
    }

    private List<String> expandGoalCode(String goalCode) {
        return switch (goalCode) {
            case Constants.FOOD_ADVICE_FOR.BABY_6_TO_8_MONTHS -> List.of(
                    goalCode,
                    Constants.FOOD_ADVICE_FOR.BABY_6_TO_8_MONTHS_DEVELOPMENT
            );
            case Constants.FOOD_ADVICE_FOR.BABY_9_TO_11_MONTHS -> List.of(
                    goalCode,
                    Constants.FOOD_ADVICE_FOR.BABY_9_TO_11_MONTHS_DEVELOPMENT
            );
            case Constants.FOOD_ADVICE_FOR.BABY_12_TO_18_MONTHS -> List.of(
                    goalCode,
                    Constants.FOOD_ADVICE_FOR.BABY_12_TO_18_MONTHS_DEVELOPMENT
            );
            case Constants.FOOD_ADVICE_FOR.BABY_19_TO_24_MONTHS -> List.of(
                    goalCode,
                    Constants.FOOD_ADVICE_FOR.BABY_19_TO_24_MONTHS_DEVELOPMENT
            );
            default -> List.of(goalCode);
        };
    }

    private List<String> babyAdvanceForByAge(Integer ageMonths) {
        int months = ageMonths != null ? ageMonths : 12;
        if (months <= 8) {
            return List.of(Constants.FOOD_ADVICE_FOR.BABY_6_TO_8_MONTHS, Constants.FOOD_ADVICE_FOR.BABY_6_TO_8_MONTHS_DEVELOPMENT);
        }
        if (months <= 11) {
            return List.of(Constants.FOOD_ADVICE_FOR.BABY_9_TO_11_MONTHS, Constants.FOOD_ADVICE_FOR.BABY_9_TO_11_MONTHS_DEVELOPMENT);
        }
        if (months <= 18) {
            return List.of(Constants.FOOD_ADVICE_FOR.BABY_12_TO_18_MONTHS, Constants.FOOD_ADVICE_FOR.BABY_12_TO_18_MONTHS_DEVELOPMENT);
        }
        return List.of(Constants.FOOD_ADVICE_FOR.BABY_19_TO_24_MONTHS, Constants.FOOD_ADVICE_FOR.BABY_19_TO_24_MONTHS_DEVELOPMENT);
    }

    private void validateBabyPlanningAge(Profile profile) {
        if (isMotherProfile(profile)) {
            return;
        }
        Integer ageMonths = resolveAgeMonths(profile);
        if (ageMonths == null || ageMonths < MIN_COMPLEMENTARY_FEEDING_AGE_MONTHS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bé dưới 6 tháng nên bú mẹ hoàn toàn; chưa thể tạo lịch ăn bổ sung"
            );
        }
    }

    private void validateTdeeAvailable(HealthRecord latestHealth, Profile profile) {
        if (latestHealth == null || latestHealth.getTdee() == null || latestHealth.getTdee() <= 0) {
            String message = isMotherProfile(profile)
                    ? "Cần cập nhật chiều cao, cân nặng và mức vận động để tính TDEE trước khi tạo lịch ăn"
                    : "Cần cập nhật cân nặng và ngày sinh để tính EER trước khi tạo lịch ăn bổ sung";
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }

    private String resolveFeedingStage(Profile profile) {
        if (isMotherProfile(profile)) {
            return "MOTHER";
        }
        Integer ageMonths = resolveAgeMonths(profile);
        if (ageMonths == null) {
            return "UNKNOWN";
        }
        if (ageMonths <= 8) {
            return "WHO_COMPLEMENTARY_6_8_MONTHS";
        }
        if (ageMonths <= 11) {
            return "WHO_COMPLEMENTARY_9_11_MONTHS";
        }
        if (ageMonths <= 23) {
            return "WHO_COMPLEMENTARY_12_23_MONTHS";
        }
        return "BEYOND_WHO_COMPLEMENTARY_RANGE";
    }

    private Double resolveComplementaryFoodEnergyTarget(Profile profile) {
        if (isMotherProfile(profile)) {
            return null;
        }
        Integer ageMonths = resolveAgeMonths(profile);
        if (ageMonths == null || ageMonths < MIN_COMPLEMENTARY_FEEDING_AGE_MONTHS) {
            return null;
        }
        if (ageMonths <= 8) {
            return WHO_COMPLEMENTARY_KCAL_6_TO_8_MONTHS;
        }
        if (ageMonths <= 11) {
            return WHO_COMPLEMENTARY_KCAL_9_TO_11_MONTHS;
        }
        if (ageMonths <= 23) {
            return WHO_COMPLEMENTARY_KCAL_12_TO_23_MONTHS;
        }
        return null;
    }

    private AiPlan appendComplementaryFeedingWarning(AiPlan plan, Profile profile) {
        Double complementaryTarget = resolveComplementaryFoodEnergyTarget(profile);
        if (complementaryTarget == null) {
            return plan;
        }

        List<String> warnings = new ArrayList<>(plan.warnings() == null ? List.of() : plan.warnings());
        Integer ageMonths = resolveAgeMonths(profile);
        String warning = ageMonths != null && ageMonths < 12
                ? "Bé từ 6 đến 11 tháng tuổi, sữa mẹ vẫn là dinh dưỡng chính; ăn dặm chỉ là phần bổ sung."
                : "Kcal chỉ tính phần ăn bổ sung; tiếp tục bú mẹ theo nhu cầu.";
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
        return new AiPlan(plan.status(), plan.summary(), warnings, plan.days(), plan.model());
    }

    private List<String> motherAdvanceForValues() {
        return List.of(
                Constants.FOOD_ADVICE_FOR.MOM_GET_BACK_IN_SHAPE,
                Constants.FOOD_ADVICE_FOR.MOM_CHANGE_DIET,
                Constants.FOOD_ADVICE_FOR.MOM_POSTPARTUM_BREASTFEEDING,
                Constants.FOOD_ADVICE_FOR.MOM_HEALTHY_ENERGY,
                Constants.FOOD_ADVICE_FOR.MOM_DIGESTION_RECOVERY,
                Constants.FOOD_ADVICE_FOR.MOM_INCREASE_MILK_SUPPLY,
                Constants.FOOD_ADVICE_FOR.MOM_SLEEP_STRESS_SUPPORT
        );
    }

    private boolean isFoodForProfile(FoodLibrary food, Profile profile) {
        String advanceFor = food.getAdvanceFor() != null ? food.getAdvanceFor().trim().toUpperCase(Locale.ROOT) : "";
        return isMotherProfile(profile) ? advanceFor.startsWith("FOR_MOTHER_") : advanceFor.startsWith("FOR_BABY_");
    }

    private NutritionPlanResponse mapPlan(NutritionPlan plan, boolean cached) {
        PlanAdjustmentContext adjustment = buildPlanAdjustmentContext(plan);
        List<NutritionPlanDayResponse> days = plan.getDays() == null ? List.of() : plan.getDays().stream()
                .sorted(Comparator.comparing(NutritionPlanDay::getPlanDate).thenComparing(NutritionPlanDay::getDayIndex))
                .map(day -> NutritionPlanDayResponse.builder()
                        .id(day.getId())
                        .date(day.getPlanDate().toString())
                        .dayIndex(day.getDayIndex())
                        .note(day.getNote())
                        .meals(day.getMeals() == null ? List.of() : day.getMeals().stream()
                                .map(meal -> mapMeal(meal, adjustment))
                                .toList())
                        .build())
                .toList();
        return NutritionPlanResponse.builder()
                .status("SUCCESS")
                .lifecycleStatus(resolvePlanLifecycleStatus(plan))
                .planId(plan.getId())
                .profileId(plan.getProfile().getId())
                .profileName(plan.getProfile().getName())
                .profileType(plan.getProfile().getProfileType())
                .currentGoal(plan.getCurrentGoal())
                .goalCode(plan.getGoalCode())
                .userNotes(plan.getFutureGoal())
                .targetDailyCalories(adjustment.targetDailyCalories())
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

    private String resolvePlanLifecycleStatus(NutritionPlan plan) {
        if (Constants.TABLE_STATUS.ACTIVE.equals(plan.getStatus())) {
            return "ACTIVE";
        }
        if (Constants.TABLE_STATUS.SUCCESS.equals(plan.getStatus())) {
            return "COMPLETED";
        }
        return "UNKNOWN";
    }

    private NutritionPlanMealResponse mapMeal(NutritionPlanMeal meal, PlanAdjustmentContext adjustment) {
        FoodLibrary food = meal.getFoodLibrary();
        Map<String, String> ingredientAmounts = food != null
                ? adjustment.ingredientAmountsByFoodId().getOrDefault(food.getId(), Map.of())
                : Map.of();
        return NutritionPlanMealResponse.builder()
                .id(meal.getId())
                .mealType(meal.getMealType())
                .foodId(food != null ? food.getId() : null)
                .foodName(food != null ? food.getName() : meal.getFoodNameSnapshot())
                .foodImageUrl(food != null ? food.getImageUrl() : null)
                .nutrition(food != null ? mapNutrition(food.getNutritionSummary()) : null)
                .adjustedCalories(food != null ? adjustment.adjustedCaloriesByFoodId().get(food.getId()) : null)
                .portionScale(adjustment.portionScale())
                .ingredientStorageKey(food != null
                        ? "babyoi_food_detail_amounts:" + meal.getPlanDay().getPlan().getProfile().getId() + ":" + food.getId()
                        : null)
                .ingredientAmounts(ingredientAmounts)
                .portion(meal.getPortion())
                .reason(meal.getReason())
                .warning(meal.getWarning())
                .eatenStatus(meal.getEatenStatus())
                .build();
    }

    private PlanAdjustmentContext buildPlanAdjustmentContext(NutritionPlan plan) {
        List<NutritionPlanDay> days = plan.getDays() == null ? List.of() : plan.getDays();
        Set<Long> foodIds = days.stream()
                .filter(Objects::nonNull)
                .flatMap(day -> day.getMeals() == null ? java.util.stream.Stream.empty() : day.getMeals().stream())
                .map(NutritionPlanMeal::getFoodLibrary)
                .filter(Objects::nonNull)
                .map(FoodLibrary::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, FoodLibrary> foodsById = days.stream()
                .filter(Objects::nonNull)
                .flatMap(day -> day.getMeals() == null ? java.util.stream.Stream.empty() : day.getMeals().stream())
                .map(NutritionPlanMeal::getFoodLibrary)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(FoodLibrary::getId, food -> food, (first, ignored) -> first, LinkedHashMap::new));
        Map<Long, List<FoodLibraryIngredient>> ingredientsByFoodId = new LinkedHashMap<>();
        Map<Long, Double> baseCaloriesByFoodId = new LinkedHashMap<>();
        for (Long foodId : foodIds) {
            List<FoodLibraryIngredient> ingredients = foodLibraryIngredientRepository.findByFoodLibraryId(foodId)
                    .stream()
                    .filter(ingredient -> !Constants.TABLE_STATUS.DELETED.equals(ingredient.getStatus()))
                    .filter(ingredient -> ingredient.getAmountPerServing() != null && ingredient.getAmountPerServing() > 0)
                    .toList();
            ingredientsByFoodId.put(foodId, ingredients);
            double ingredientCalories = ingredients.stream()
                    .mapToDouble(this::ingredientCaloriesAtBaseServing)
                    .sum();
            if (ingredientCalories <= 0) {
                FoodLibrary food = foodsById.get(foodId);
                ingredientCalories = food != null && food.getNutritionSummary() != null
                        && food.getNutritionSummary().getTotalCalories() != null
                        ? food.getNutritionSummary().getTotalCalories()
                        : 0D;
            }
            if (ingredientCalories > 0) {
                baseCaloriesByFoodId.put(foodId, ingredientCalories);
            }
        }
        List<Double> dailyBaseCalories = days.stream()
                .map(day -> day.getMeals() == null ? 0D : day.getMeals().stream()
                        .map(NutritionPlanMeal::getFoodLibrary)
                        .filter(Objects::nonNull)
                        .mapToDouble(food -> baseCaloriesByFoodId.getOrDefault(
                                food.getId(),
                                food.getNutritionSummary() != null && food.getNutritionSummary().getTotalCalories() != null
                                        ? food.getNutritionSummary().getTotalCalories()
                                        : 0D
                        ))
                        .sum())
                .toList();
        double maximumBaseCalories = dailyBaseCalories.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0D);
        HealthRecord latestHealth = healthRecordRepository
                .findFirstByProfileIdOrderByRecordDateDescIdDesc(plan.getProfile().getId())
                .orElse(null);
        Double tdee = latestHealth != null ? latestHealth.getTdee() : null;
        Double targetDailyCalories;
        if (isMotherProfile(plan.getProfile())) {
            targetDailyCalories = tdee != null && tdee > 0
                    ? roundOneDecimal(tdee * resolveGoalEnergyMultiplier(plan.getCurrentGoal(), plan.getGoalCode()))
                    : null;
        } else {
            Double complementaryTarget = resolveComplementaryFoodEnergyTarget(plan.getProfile());
            targetDailyCalories = complementaryTarget != null
                    ? roundOneDecimal(tdee != null && tdee > 0 ? Math.min(tdee, complementaryTarget) : complementaryTarget)
                    : tdee;
        }
        double portionScale = targetDailyCalories != null && maximumBaseCalories > 0
                ? clamp(targetDailyCalories / maximumBaseCalories, 0.05D, 1.5D)
                : 1D;
        portionScale = floorThreeDecimals(portionScale);

        Map<Long, Map<String, String>> ingredientAmountsByFoodId = new LinkedHashMap<>();
        Map<Long, Double> adjustedCaloriesByFoodId = new LinkedHashMap<>();
        for (Long foodId : foodIds) {
            Map<String, String> amounts = new LinkedHashMap<>();
            double adjustedCalories = 0D;
            for (FoodLibraryIngredient ingredient : ingredientsByFoodId.getOrDefault(foodId, List.of())) {
                String adjustedAmount = formatAmountDown(ingredient.getAmountPerServing() * portionScale);
                amounts.put(
                        String.valueOf(ingredient.getId()),
                        adjustedAmount
                );
                adjustedCalories += ingredientCaloriesAtAmount(ingredient, Double.parseDouble(adjustedAmount));
            }
            if (!amounts.isEmpty()) {
                ingredientAmountsByFoodId.put(foodId, amounts);
            }
            Double baseCalories = baseCaloriesByFoodId.get(foodId);
            if (adjustedCalories > 0) {
                adjustedCaloriesByFoodId.put(foodId, floorOneDecimal(adjustedCalories));
            } else if (baseCalories != null && baseCalories > 0) {
                adjustedCaloriesByFoodId.put(foodId, floorOneDecimal(baseCalories * portionScale));
            }
        }
        return new PlanAdjustmentContext(
                targetDailyCalories,
                portionScale,
                ingredientAmountsByFoodId,
                adjustedCaloriesByFoodId
        );
    }

    private double ingredientCaloriesAtBaseServing(FoodLibraryIngredient ingredient) {
        IngredientNutrition nutrition = ingredient.getFoodIngredient() != null
                ? ingredient.getFoodIngredient().getNutrition()
                : null;
        if (nutrition == null || nutrition.getCalories() == null || nutrition.getBaseAmount() == null
                || nutrition.getBaseAmount() <= 0 || ingredient.getAmountPerServing() == null) {
            return 0D;
        }
        return nutrition.getCalories() * ingredient.getAmountPerServing() / nutrition.getBaseAmount();
    }

    private double ingredientCaloriesAtAmount(FoodLibraryIngredient ingredient, double amount) {
        IngredientNutrition nutrition = ingredient.getFoodIngredient() != null
                ? ingredient.getFoodIngredient().getNutrition()
                : null;
        if (nutrition == null || nutrition.getCalories() == null || nutrition.getBaseAmount() == null
                || nutrition.getBaseAmount() <= 0 || amount <= 0) {
            return 0D;
        }
        return nutrition.getCalories() * amount / nutrition.getBaseAmount();
    }

    private double resolveGoalEnergyMultiplier(String goal, String goalCode) {
        if (Constants.FOOD_ADVICE_FOR.MOM_GET_BACK_IN_SHAPE.equals(goalCode) || isWeightLossGoal(goal)) {
            return 0.85D;
        }
        if (containsNormalized(goal, "giu dang")) {
            return 0.95D;
        }
        if (Constants.FOOD_ADVICE_FOR.MOM_HEALTHY_ENERGY.equals(goalCode)
                || Constants.FOOD_ADVICE_FOR.MOM_INCREASE_MILK_SUPPLY.equals(goalCode)
                || containsNormalized(goal, "tang can")
                || containsNormalized(goal, "nang luong")
                || containsNormalized(goal, "loi sua")) {
            return 1D;
        }
        if (Constants.FOOD_ADVICE_FOR.MOM_DIGESTION_RECOVERY.equals(goalCode)
                || containsNormalized(goal, "de tieu")
                || containsNormalized(goal, "tieu hoa")) {
            return 0.95D;
        }
        return 1D;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Double roundOneDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private double floorThreeDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.DOWN).doubleValue();
    }

    private Double floorOneDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.DOWN).doubleValue();
    }

    private String formatAmountDown(double amount) {
        BigDecimal value = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.DOWN)
                .max(BigDecimal.valueOf(0.01D))
                .stripTrailingZeros()
                ;
        return value.toPlainString();
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

    private void validatePlanEditable(NutritionPlan plan) {
        if (plan == null || !Constants.TABLE_STATUS.ACTIVE.equals(plan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch ăn");
        }
    }

    private void validatePlanDeletable(NutritionPlan plan) {
        if (plan == null || (!Constants.TABLE_STATUS.ACTIVE.equals(plan.getStatus())
                && !Constants.TABLE_STATUS.SUCCESS.equals(plan.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch ăn");
        }
    }

    private void validateCurrentUser(Long userId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof CustomUserDetails userDetails && !userDetails.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
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

    private int resolveMealsPerDay(Integer mealsPerDay, Profile profile) {
        if (isMotherProfile(profile)) {
            int value = mealsPerDay != null ? mealsPerDay : 4;
            if (value < 3 || value > 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mealsPerDay của mẹ phải từ 3 đến 6");
            }
            return value;
        }

        Integer ageMonths = resolveAgeMonths(profile);
        if (ageMonths != null && ageMonths < 6) {
            return 1;
        }
        if (ageMonths != null && ageMonths <= 8) {
            return 2;
        }
        if (ageMonths != null && ageMonths <= 24) {
            return 3;
        }

        int value = mealsPerDay != null ? mealsPerDay : 4;
        if (value < 3 || value > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mealsPerDay phải từ 3 đến 6");
        }
        return value;
    }

    private List<String> resolveMealTypes(List<String> requestedMealTypes, int mealsPerDay) {
        List<String> mealTypes = requestedMealTypes == null || requestedMealTypes.isEmpty()
                ? defaultMealTypes(mealsPerDay)
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

    private List<String> defaultMealTypes(int mealsPerDay) {
        return switch (mealsPerDay) {
            case 1 -> List.of("BREAKFAST");
            case 2 -> List.of("BREAKFAST", "LUNCH");
            case 3 -> List.of("BREAKFAST", "LUNCH", "DINNER");
            case 4 -> List.of("BREAKFAST", "MORNING_SNACK", "LUNCH", "DINNER");
            case 5 -> List.of("BREAKFAST", "MORNING_SNACK", "LUNCH", "AFTERNOON_SNACK", "DINNER");
            default -> DEFAULT_MEAL_TYPES;
        };
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

    private void validateGoalTarget(Profile profile, NutritionPlanGenerateRequest request) {
        if (!isMotherProfile(profile) || !isWeightLossGoal(request.getCurrentGoal())) {
            return;
        }
        Double targetWeightKg = request.getTargetWeightKg();
        if (targetWeightKg == null || targetWeightKg < 30D || targetWeightKg > 200D) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cân nặng hướng đến là bắt buộc khi mục tiêu là giảm cân");
        }
    }

    private boolean isWeightLossGoal(String goal) {
        return containsNormalized(goal, "giam can");
    }

    private boolean containsNormalized(String value, String token) {
        return normalizedText(value).contains(normalizedText(token));
    }

    private String normalizedText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
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

    private String extractFinishReason(JsonNode response) {
        if (response == null) {
            return "UNKNOWN";
        }
        JsonNode candidates = response.get("candidates");
        if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
            return candidates.get(0).path("finishReason").asText("UNKNOWN");
        }
        return "UNKNOWN";
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
        return timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 40L;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
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

    private record PlanAdjustmentContext(
            Double targetDailyCalories,
            double portionScale,
            Map<Long, Map<String, String>> ingredientAmountsByFoodId,
            Map<Long, Double> adjustedCaloriesByFoodId
    ) {
    }
}
