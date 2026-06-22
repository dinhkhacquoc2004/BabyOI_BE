package com.example.babyoi_be.service.impl;

import com.example.babyoi_be.common.Constants;
import com.example.babyoi_be.domain.dto.request.AgenticRagChatHistoryItem;
import com.example.babyoi_be.domain.dto.request.AiChatRequest;
import com.example.babyoi_be.domain.dto.respone.AiChatConversationResponse;
import com.example.babyoi_be.domain.dto.respone.AiChatMessageResponse;
import com.example.babyoi_be.domain.dto.respone.AiChatResponse;
import com.example.babyoi_be.domain.dto.respone.AgenticRagChatResponse;
import com.example.babyoi_be.domain.entity.AiChatConversation;
import com.example.babyoi_be.domain.entity.AiChatMessage;
import com.example.babyoi_be.domain.entity.BabyRoutineEntry;
import com.example.babyoi_be.domain.entity.FoodLibrary;
import com.example.babyoi_be.domain.entity.FoodLibraryIngredient;
import com.example.babyoi_be.domain.entity.FoodNutritionSummary;
import com.example.babyoi_be.domain.entity.HealthRecord;
import com.example.babyoi_be.domain.entity.IllnessEvent;
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.RestrictedFood;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.AiChatConversationRepository;
import com.example.babyoi_be.repository.AiChatMessageRepository;
import com.example.babyoi_be.repository.BabyRoutineEntryRepository;
import com.example.babyoi_be.repository.FoodLibraryRepository;
import com.example.babyoi_be.repository.HealthRecordRepository;
import com.example.babyoi_be.repository.IllnessEventRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.repository.RestrictedFoodRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.AgenticRagClient;
import com.example.babyoi_be.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final String SENDER_USER = "USER";
    private static final String SENDER_ASSISTANT = "ASSISTANT";
    private static final long MOTHER_FOOD_FUNCTION_CODE = 1L;
    private static final long CHILD_FOOD_FUNCTION_CODE = 2L;
    private static final Map<String, List<String>> CHILD_FOOD_AGE_GROUPS = Map.of(
            "FOR_BABY_6_8_MONTHS", List.of("FOR_BABY_6_8_MONTHS", "FOR_BABY_6_8_MONTHS_DEVELOPMENT"),
            "FOR_BABY_9_11_MONTHS", List.of("FOR_BABY_9_11_MONTHS", "FOR_BABY_9_11_MONTHS_DEVELOPMENT"),
            "FOR_BABY_12_18_MONTHS", List.of("FOR_BABY_12_18_MONTHS", "FOR_BABY_12_18_MONTHS_DEVELOPMENT"),
            "FOR_BABY_19_24_MONTHS", List.of("FOR_BABY_19_24_MONTHS", "FOR_BABY_19_24_MONTHS_DEVELOPMENT")
    );

    private final AiChatConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;
    private final BabyRoutineEntryRepository babyRoutineEntryRepository;
    private final ProfileRepository profileRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final IllnessEventRepository illnessEventRepository;
    private final FoodLibraryRepository foodLibraryRepository;
    private final RestrictedFoodRepository restrictedFoodRepository;
    private final AgenticRagClient agenticRagClient;

    @Override
    @Transactional
    public AiChatResponse sendMessage(AiChatRequest request) {
        CustomUserDetails currentUser = getCurrentUser();
        Profile profile = request.getProfileId() != null ? resolveProfile(request.getProfileId(), currentUser) : null;
        String conversationId = normalizeConversationId(request.getConversationId());
        String userMessage = request.getMessage().trim();

        AiChatConversation conversation = conversationRepository.findByConversationIdAndStatus(
                        conversationId,
                        Constants.TABLE_STATUS.ACTIVE
                )
                .map(existingConversation -> validateConversationOwner(existingConversation, currentUser, profile))
                .orElseGet(() -> createConversation(conversationId, currentUser.getUser(), profile, userMessage));

        List<AgenticRagChatHistoryItem> chatHistory = buildChatHistory(conversation);
        Map<String, Object> profileContext = buildProfileContext(
                profile,
                buildCatalogQuery(userMessage, chatHistory)
        );

        LocalDateTime now = LocalDateTime.now();
        messageRepository.save(AiChatMessage.builder()
                .conversation(conversation)
                .sender(SENDER_USER)
                .message(userMessage)
                .status(Constants.TABLE_STATUS.ACTIVE)
                .createdAt(now)
                .build());

        AgenticRagChatResponse ragResponse = agenticRagClient.chat(
                conversation.getConversationId(),
                resolveUserName(currentUser),
                userMessage,
                profileContext,
                chatHistory
        );

        String botReply = normalizeBotReply(ragResponse);
        AiChatMessage assistantMessage = messageRepository.save(AiChatMessage.builder()
                .conversation(conversation)
                .sender(SENDER_ASSISTANT)
                .message(botReply)
                .selectedAgent(ragResponse.getSelectedAgent())
                .intent(ragResponse.getIntent())
                .safetyLevel(ragResponse.getSafetyLevel())
                .usedDomains(writeUsedDomains(ragResponse.getUsedDomains()))
                .debugLog(ragResponse.getDebugLog())
                .rawResult(writeMap(ragResponse.getRawResult()))
                .status(Constants.TABLE_STATUS.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build());

        conversation.setUpdatedAt(assistantMessage.getCreatedAt());
        conversationRepository.save(conversation);

        return AiChatResponse.builder()
                .conversationId(conversation.getConversationId())
                .profileId(profile != null ? profile.getId() : null)
                .userMessage(userMessage)
                .botReply(botReply)
                .answer(ragResponse.getAnswer())
                .selectedAgent(ragResponse.getSelectedAgent())
                .intent(ragResponse.getIntent())
                .safetyLevel(ragResponse.getSafetyLevel())
                .usedDomains(ragResponse.getUsedDomains())
                .createdAt(assistantMessage.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiChatConversationResponse> getConversations(Long profileId) {
        CustomUserDetails currentUser = getCurrentUser();
        if (profileId != null) {
            resolveProfile(profileId, currentUser);
        }

        List<AiChatConversation> conversations = profileId == null
                ? conversationRepository.findByUserIdAndStatusOrderByUpdatedAtDescCreatedAtDescIdDesc(
                        currentUser.getId(),
                        Constants.TABLE_STATUS.ACTIVE
                )
                : conversationRepository.findByProfileIdAndUserIdAndStatusOrderByUpdatedAtDesc(
                        profileId,
                        currentUser.getId(),
                        Constants.TABLE_STATUS.ACTIVE
                );

        return conversations.stream()
                .map(this::mapConversation)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiChatMessageResponse> getMessages(String conversationId) {
        CustomUserDetails currentUser = getCurrentUser();
        AiChatConversation conversation = resolveConversation(conversationId, currentUser);
        return messageRepository.findByConversationConversationIdAndConversationUserIdAndStatusOrderByCreatedAtAsc(
                        conversation.getConversationId(),
                        currentUser.getId(),
                        Constants.TABLE_STATUS.ACTIVE
                )
                .stream()
                .map(this::mapMessage)
                .toList();
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId) {
        CustomUserDetails currentUser = getCurrentUser();
        AiChatConversation conversation = resolveConversation(conversationId, currentUser);
        conversation.setStatus(Constants.TABLE_STATUS.DELETED);
        conversation.setUpdatedAt(LocalDateTime.now());
        messageRepository.findByConversationIdAndStatus(conversation.getId(), Constants.TABLE_STATUS.ACTIVE)
                .forEach(message -> message.setStatus(Constants.TABLE_STATUS.DELETED));
        conversationRepository.save(conversation);
    }

    private AiChatConversation createConversation(String conversationId, Users user, Profile profile, String userMessage) {
        LocalDateTime now = LocalDateTime.now();
        return conversationRepository.save(AiChatConversation.builder()
                .conversationId(conversationId)
                .user(user)
                .profile(profile)
                .title(buildTitle(userMessage))
                .status(Constants.TABLE_STATUS.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private AiChatConversation validateConversationOwner(
            AiChatConversation conversation,
            CustomUserDetails currentUser,
            Profile requestedProfile
    ) {
        if (conversation.getUser() == null || !currentUser.getId().equals(conversation.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
        if (requestedProfile != null) {
            Long conversationProfileId = conversation.getProfile() != null ? conversation.getProfile().getId() : null;
            if (!requestedProfile.getId().equals(conversationProfileId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation does not belong to this profile");
            }
        }
        return conversation;
    }

    private AiChatConversation resolveConversation(String conversationId, CustomUserDetails currentUser) {
        String normalizedConversationId = normalizeRequiredConversationId(conversationId);
        AiChatConversation conversation = conversationRepository.findByConversationIdAndStatus(
                        normalizedConversationId,
                        Constants.TABLE_STATUS.ACTIVE
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        return validateConversationOwner(conversation, currentUser, null);
    }

    private Profile resolveProfile(Long profileId, CustomUserDetails currentUser) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!Constants.TABLE_STATUS.ACTIVE.equals(profile.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile is not active");
        }
        if (profile.getUser() == null || !currentUser.getId().equals(profile.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "auth.forbidden");
        }
        return profile;
    }

    private CustomUserDetails getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userDetails;
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId.trim();
    }

    private String normalizeRequiredConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation id is required");
        }
        return conversationId.trim();
    }

    private String resolveUserName(CustomUserDetails currentUser) {
        String realName = currentUser.getRealName();
        return realName == null || realName.isBlank() ? currentUser.getUsername() : realName;
    }

    private String normalizeBotReply(AgenticRagChatResponse ragResponse) {
        String botReply = ragResponse.getBotReply();
        if (botReply == null || botReply.isBlank()) {
            botReply = ragResponse.getAnswer();
        }
        if (botReply == null || botReply.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AgenticRAG response is missing bot reply");
        }
        return botReply.trim();
    }

    private Map<String, Object> buildProfileContext(Profile profile, String currentQuestion) {
        if (profile == null) {
            return null;
        }
        Map<String, Object> profileContext = new LinkedHashMap<>();
        profileContext.put("id", profile.getId());
        profileContext.put("name", profile.getName());
        profileContext.put("dateOfBirth", profile.getDateOfBirth());
        profileContext.put("ageMonths", calculateAgeMonths(profile));
        profileContext.put("sex", profile.getSex() != null ? profile.getSex().name() : null);
        profileContext.put("profileType", profile.getProfileType());
        profileContext.put("profileCode", profile.getProfileCode());
        appendFoodCatalogContext(profileContext, profile, currentQuestion);
        List<HealthRecord> recentHealthRecords = healthRecordRepository
                .findTop2ByProfileIdOrderByRecordDateDescIdDesc(profile.getId());
        if (!recentHealthRecords.isEmpty()) {
            appendHealthContext(profileContext, recentHealthRecords.get(0));
            if (recentHealthRecords.size() > 1) {
                appendPreviousHealthContext(profileContext, recentHealthRecords.get(0), recentHealthRecords.get(1));
            }
        }
        appendRecentIllnessContext(profileContext, profile);
        appendTodayRoutineContext(profileContext, profile);
        return profileContext;
    }

    private void appendTodayRoutineContext(Map<String, Object> profileContext, Profile profile) {
        if (!"CHILD".equalsIgnoreCase(profile.getProfileType())) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<BabyRoutineEntry> entries =
                babyRoutineEntryRepository.findByProfile_IdAndRoutineDateAndStatusOrderByPlannedTimeAscIdAsc(
                        profile.getId(),
                        today,
                        Constants.TABLE_STATUS.ACTIVE
                );
        if (entries.isEmpty()) {
            return;
        }
        String routine = entries.stream()
                .limit(16)
                .map(entry -> {
                    String actual = entry.getActualTime() != null
                            ? ", thực tế " + entry.getActualTime()
                            : "";
                    String completion = Boolean.TRUE.equals(entry.getCompleted())
                            ? ", đã hoàn thành"
                            : ", chưa hoàn thành";
                    return entry.getPlannedTime() + " " + sanitizeCatalogValue(entry.getActivity())
                            + " (" + sanitizeCatalogValue(entry.getType()) + actual + completion + ")";
                })
                .collect(Collectors.joining(" ;; "));
        profileContext.put("profileRoutineDate", today);
        profileContext.put("profileRoutineToday", routine);
        profileContext.put("profileRoutineCount", entries.size());
    }

    private void appendFoodCatalogContext(
            Map<String, Object> profileContext,
            Profile profile,
            String currentQuestion
    ) {
        Long ageMonths = calculateAgeMonths(profile);
        boolean childProfile = "CHILD".equalsIgnoreCase(profile.getProfileType());
        String ageGroup = childProfile ? resolveChildFoodAgeGroup(ageMonths) : "MOTHER";
        List<FoodLibrary> foods;

        if (childProfile) {
            List<String> advanceForValues = CHILD_FOOD_AGE_GROUPS.getOrDefault(ageGroup, List.of());
            foods = advanceForValues.isEmpty()
                    ? List.of()
                    : foodLibraryRepository.findByStatusAndFunctionCodeAndAdvanceForInOrderByIdAsc(
                            Constants.TABLE_STATUS.ACTIVE,
                            CHILD_FOOD_FUNCTION_CODE,
                            advanceForValues
                    );
        } else {
            foods = foodLibraryRepository.findByStatusAndFunctionCodeOrderByIdAsc(
                    Constants.TABLE_STATUS.ACTIVE,
                    MOTHER_FOOD_FUNCTION_CODE
            );
        }

        String motherGoalCode = childProfile ? null : resolveMotherGoalCode(currentQuestion);
        List<String> requestedIngredients = resolveRequestedIngredients(foods, currentQuestion);
        List<String> requestedFoodNames = resolveRequestedFoodNames(foods, currentQuestion);
        boolean analysisRequest = isFoodAnalysisRequest(currentQuestion);

        List<RestrictedFood> restrictedFoods = restrictedFoodRepository.findByProfileId(profile.getId());
        Set<Long> restrictedFoodIds = restrictedFoods.stream()
                .filter(item -> item.getFoodLibrary() != null)
                .map(item -> item.getFoodLibrary().getId())
                .collect(Collectors.toUnmodifiableSet());
        List<FoodLibrary> profileEligibleFoods = foods.stream()
                .filter(food -> !restrictedFoodIds.contains(food.getId()))
                .toList();
        List<FoodLibrary> allowedFoods = profileEligibleFoods.stream()
                .filter(food -> motherGoalCode == null || motherGoalCode.equalsIgnoreCase(food.getAdvanceFor()))
                .filter(food -> requestedIngredients.isEmpty() || containsAllIngredients(food, requestedIngredients))
                .filter(food -> requestedFoodNames.isEmpty() || requestedFoodNames.contains(food.getName()))
                .limit(analysisRequest ? 5 : 30)
                .toList();

        profileContext.put("foodAgeGroup", ageGroup);
        profileContext.put("profileFoodCatalog", formatFoodCatalog(allowedFoods, analysisRequest));
        profileContext.put("profileFoodCatalogCount", allowedFoods.size());
        profileContext.put("profileFoodCatalogIndex", formatFoodCatalogIndex(
                profileEligibleFoods.stream().limit(160).toList()
        ));
        appendCrossSubjectCatalogs(profileContext, childProfile, profileEligibleFoods);
        profileContext.put("catalogGoalCode", motherGoalCode);
        profileContext.put("catalogGoalLabel", motherGoalLabel(motherGoalCode));
        profileContext.put("requestedIngredients", String.join(" ;; ", requestedIngredients));
        profileContext.put("requestedFoodNames", String.join(" ;; ", requestedFoodNames));
        profileContext.put("foodAnalysisRequested", analysisRequest);
        profileContext.put("restrictedFoods", restrictedFoods.stream()
                .filter(item -> item.getFoodLibrary() != null)
                .map(item -> sanitizeCatalogValue(item.getFoodLibrary().getName()))
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(" ;; ")));
        profileContext.put("restrictedFoodCount", restrictedFoods.size());
    }

    private void appendCrossSubjectCatalogs(
            Map<String, Object> profileContext,
            boolean selectedProfileIsChild,
            List<FoodLibrary> selectedProfileFoods
    ) {
        List<FoodLibrary> motherFoods = selectedProfileIsChild
                ? foodLibraryRepository.findByStatusAndFunctionCodeOrderByIdAsc(
                        Constants.TABLE_STATUS.ACTIVE,
                        MOTHER_FOOD_FUNCTION_CODE
                )
                : selectedProfileFoods;
        List<FoodLibrary> childFoods = selectedProfileIsChild
                ? selectedProfileFoods
                : foodLibraryRepository.findByStatusAndFunctionCodeOrderByIdAsc(
                        Constants.TABLE_STATUS.ACTIVE,
                        CHILD_FOOD_FUNCTION_CODE
                );

        profileContext.put("motherFoodCatalogIndex", formatFoodCatalogIndex(
                motherFoods.stream().limit(160).toList()
        ));
        profileContext.put("childFoodCatalogIndex", formatFoodCatalogIndex(
                childFoods.stream().limit(160).toList()
        ));
    }

    private String resolveChildFoodAgeGroup(Long ageMonths) {
        if (ageMonths == null || ageMonths < 6) {
            return "BELOW_6_MONTHS";
        }
        if (ageMonths <= 8) {
            return "FOR_BABY_6_8_MONTHS";
        }
        if (ageMonths <= 11) {
            return "FOR_BABY_9_11_MONTHS";
        }
        if (ageMonths <= 18) {
            return "FOR_BABY_12_18_MONTHS";
        }
        if (ageMonths <= 24) {
            return "FOR_BABY_19_24_MONTHS";
        }
        return "ABOVE_24_MONTHS";
    }

    private String formatFoodCatalog(List<FoodLibrary> foods, boolean includeAnalysisDetails) {
        return foods.stream()
                .map(food -> {
                    FoodNutritionSummary nutrition = food.getNutritionSummary();
                    String calories = nutrition != null && nutrition.getTotalCalories() != null
                            ? Math.round(nutrition.getTotalCalories()) + " kcal"
                            : "";
                    String protein = nutrition != null && nutrition.getTotalProtein() != null
                            ? String.format(java.util.Locale.ROOT, "%.1f g protein", nutrition.getTotalProtein())
                            : "";
                    String ingredients = food.getIngredients() == null ? "" : food.getIngredients().stream()
                            .filter(item -> item.getFoodIngredient() != null)
                            .map(item -> sanitizeCatalogValue(item.getFoodIngredient().getNameIngredients()))
                            .filter(name -> !name.isBlank())
                            .distinct()
                            .collect(Collectors.joining(", "));
                    FoodNutritionSummary summary = food.getNutritionSummary();
                    String carbs = nutritionValue(summary != null ? summary.getTotalCarbs() : null, "g carbs");
                    String fat = nutritionValue(summary != null ? summary.getTotalFat() : null, "g fat");
                    String fiber = nutritionValue(summary != null ? summary.getTotalFiber() : null, "g fiber");
                    String sugar = nutritionValue(summary != null ? summary.getTotalSugar() : null, "g sugar");
                    String sodium = nutritionValue(summary != null ? summary.getTotalSodium() : null, "mg sodium");
                    String goodPoints = includeAnalysisDetails && food.getRecommendation() != null
                            ? compactCatalogText(food.getRecommendation().getGoodPoints()) : "";
                    String badPoints = includeAnalysisDetails && food.getRecommendation() != null
                            ? compactCatalogText(food.getRecommendation().getBadPoints()) : "";
                    String advice = includeAnalysisDetails && food.getRecommendation() != null
                            ? compactCatalogText(food.getRecommendation().getAdvice()) : "";
                    String cookingWay = includeAnalysisDetails && food.getRecommendation() != null
                            ? compactCatalogText(food.getRecommendation().getCookingWay()) : "";
                    return food.getId() + "|" + sanitizeCatalogValue(food.getName()) + "|"
                            + sanitizeCatalogValue(food.getAdvanceFor()) + "|" + calories + "|" + protein
                            + "|" + ingredients + "|" + carbs + "|" + fat + "|" + fiber
                            + "|" + sugar + "|" + sodium + "|" + goodPoints + "|" + badPoints
                            + "|" + advice + "|" + cookingWay;
                })
                .collect(Collectors.joining(" ;; "));
    }

    private List<String> resolveRequestedFoodNames(List<FoodLibrary> foods, String question) {
        String normalizedQuestion = normalizeSearchText(question);
        if (normalizedQuestion.isBlank()) {
            return List.of();
        }
        return foods.stream()
                .filter(food -> containsNormalizedPhrase(normalizedQuestion, normalizeSearchText(food.getName())))
                .map(FoodLibrary::getName)
                .distinct()
                .limit(5)
                .toList();
    }

    private String formatFoodCatalogIndex(List<FoodLibrary> foods) {
        return foods.stream()
                .map(food -> {
                    FoodNutritionSummary nutrition = food.getNutritionSummary();
                    String calories = nutrition != null && nutrition.getTotalCalories() != null
                            ? Math.round(nutrition.getTotalCalories()) + " kcal" : "";
                    String protein = nutrition != null && nutrition.getTotalProtein() != null
                            ? String.format(java.util.Locale.ROOT, "%.1f g protein", nutrition.getTotalProtein()) : "";
                    String ingredients = food.getIngredients() == null ? "" : food.getIngredients().stream()
                            .filter(item -> item.getFoodIngredient() != null)
                            .map(item -> sanitizeCatalogValue(item.getFoodIngredient().getNameIngredients()))
                            .filter(name -> !name.isBlank())
                            .distinct()
                            .collect(Collectors.joining(", "));
                    return food.getId() + "|" + sanitizeCatalogValue(food.getName()) + "|"
                            + sanitizeCatalogValue(food.getAdvanceFor()) + "|" + calories + "|" + protein
                            + "|" + ingredients;
                })
                .collect(Collectors.joining(" ;; "));
    }

    private boolean isFoodAnalysisRequest(String question) {
        String text = normalizeSearchText(question);
        return containsAny(text, "phan tich", "danh gia", "diem manh", "diem yeu", "diem han che", "uu diem", "nhuoc diem", "luu y mon")
                || containsAny(text, "thong tin chi tiet", "chi tiet cua no", "mon nay co gi");
    }

    private String buildCatalogQuery(
            String currentQuestion,
            List<AgenticRagChatHistoryItem> chatHistory
    ) {
        if (!isReferentialFollowUp(currentQuestion)) {
            return currentQuestion;
        }
        String previousUserQuestions = chatHistory.stream()
                .filter(item -> "USER".equalsIgnoreCase(item.getSender()))
                .map(AgenticRagChatHistoryItem::getMessage)
                .filter(message -> message != null && !message.isBlank())
                .collect(Collectors.joining(" "));
        return (previousUserQuestions + " " + currentQuestion).trim();
    }

    private boolean isReferentialFollowUp(String question) {
        String text = normalizeSearchText(question);
        return containsAny(text, "mon do", "mon nay", "cai do", "cai nay", "cua no", "chi tiet cua no", "vua noi", "vua goi y");
    }

    private String nutritionValue(Double value, String unit) {
        return value == null ? "" : String.format(java.util.Locale.ROOT, "%.1f %s", value, unit);
    }

    private String compactCatalogText(String value) {
        String clean = sanitizeCatalogValue(value);
        return clean.length() <= 320 ? clean : clean.substring(0, 317) + "...";
    }

    private List<String> resolveRequestedIngredients(List<FoodLibrary> foods, String question) {
        String normalizedQuestion = normalizeSearchText(question);
        if (normalizedQuestion.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> result = foods.stream()
                .flatMap(food -> food.getIngredients() == null
                        ? java.util.stream.Stream.<FoodLibraryIngredient>empty()
                        : food.getIngredients().stream())
                .filter(item -> item.getFoodIngredient() != null)
                .map(item -> item.getFoodIngredient().getNameIngredients())
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> containsNormalizedPhrase(normalizedQuestion, normalizeSearchText(name)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!containsNormalizedPhrase(normalizedQuestion, "trung ga")
                && containsNormalizedPhrase(normalizedQuestion, "ga")) {
            result.add("gà");
        }
        if (containsNormalizedPhrase(normalizedQuestion, "bo")) result.add("bò");
        if (containsNormalizedPhrase(normalizedQuestion, "heo") || containsNormalizedPhrase(normalizedQuestion, "lon")) result.add("heo");
        if (containsNormalizedPhrase(normalizedQuestion, "ca") && result.stream().noneMatch(item -> normalizeSearchText(item).startsWith("ca "))) result.add("cá");
        return result.stream().limit(5).toList();
    }

    private boolean containsAllIngredients(FoodLibrary food, List<String> requestedIngredients) {
        if (food.getIngredients() == null) {
            return false;
        }
        List<String> foodIngredients = food.getIngredients().stream()
                .filter(item -> item.getFoodIngredient() != null)
                .map(item -> normalizeSearchText(item.getFoodIngredient().getNameIngredients()))
                .toList();
        return requestedIngredients.stream()
                .map(this::normalizeSearchText)
                .allMatch(requested -> foodIngredients.stream()
                        .anyMatch(actual -> ingredientMatches(requested, actual)));
    }

    private boolean ingredientMatches(String requested, String actual) {
        if (requested.equals(actual)) return true;
        return switch (requested) {
            case "ga" -> actual.matches(".*(?:thit|uc|dui|canh|gan) ga.*") || actual.equals("ga");
            case "bo" -> actual.contains("thit bo") || actual.equals("bo");
            case "heo", "lon" -> actual.contains("thit heo") || actual.contains("thit lon") || actual.equals("heo");
            case "ca" -> actual.startsWith("ca ") || actual.equals("ca");
            default -> false;
        };
    }

    private String resolveMotherGoalCode(String question) {
        String text = normalizeSearchText(question);
        if (containsAny(text, "tang sua", "loi sua", "nhieu sua", "kich sua")) {
            return Constants.FOOD_ADVICE_FOR.MOM_INCREASE_MILK_SUPPLY;
        }
        if (containsAny(text, "giu dang", "lay lai voc dang", "giam can", "an kieng")) {
            return Constants.FOOD_ADVICE_FOR.MOM_GET_BACK_IN_SHAPE;
        }
        if (containsAny(text, "sau sinh", "cho con bu")) {
            return Constants.FOOD_ADVICE_FOR.MOM_POSTPARTUM_BREASTFEEDING;
        }
        if (containsAny(text, "tieu hoa", "tao bon", "de tieu")) {
            return Constants.FOOD_ADVICE_FOR.MOM_DIGESTION_RECOVERY;
        }
        if (containsAny(text, "tang nang luong", "tang can", "met moi")) {
            return Constants.FOOD_ADVICE_FOR.MOM_HEALTHY_ENERGY;
        }
        if (containsAny(text, "doi mon", "da dang mon", "an ngon")) {
            return Constants.FOOD_ADVICE_FOR.MOM_CHANGE_DIET;
        }
        if (containsAny(text, "mat ngu", "giac ngu", "stress", "cang thang")) {
            return Constants.FOOD_ADVICE_FOR.MOM_SLEEP_STRESS_SUPPORT;
        }
        return null;
    }

    private String motherGoalLabel(String goalCode) {
        if (goalCode == null) return null;
        return switch (goalCode) {
            case Constants.FOOD_ADVICE_FOR.MOM_INCREASE_MILK_SUPPLY -> "tăng tiết sữa";
            case Constants.FOOD_ADVICE_FOR.MOM_GET_BACK_IN_SHAPE -> "giữ dáng/giảm cân lành mạnh";
            case Constants.FOOD_ADVICE_FOR.MOM_POSTPARTUM_BREASTFEEDING -> "sau sinh, đang cho con bú";
            case Constants.FOOD_ADVICE_FOR.MOM_DIGESTION_RECOVERY -> "hỗ trợ tiêu hóa";
            case Constants.FOOD_ADVICE_FOR.MOM_HEALTHY_ENERGY -> "tăng năng lượng";
            case Constants.FOOD_ADVICE_FOR.MOM_CHANGE_DIET -> "đổi món";
            case Constants.FOOD_ADVICE_FOR.MOM_SLEEP_STRESS_SUPPORT -> "giấc ngủ/giảm stress";
            default -> goalCode;
        };
    }

    private boolean containsAny(String text, String... phrases) {
        return Arrays.stream(phrases).anyMatch(phrase -> containsNormalizedPhrase(text, phrase));
    }

    private boolean containsNormalizedPhrase(String normalizedText, String normalizedPhrase) {
        if (normalizedPhrase == null || normalizedPhrase.isBlank()) {
            return false;
        }
        return (" " + normalizedText + " ").contains(" " + normalizedPhrase + " ");
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD)
                .replace("đ", "d")
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private String sanitizeCatalogValue(String value) {
        return value == null ? "" : value.replace("|", " ")
                .replace(";;", " ")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Long calculateAgeMonths(Profile profile) {
        if (profile.getDateOfBirth() == null) {
            return null;
        }
        LocalDate birthDate = profile.getDateOfBirth();
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            return null;
        }
        return ChronoUnit.MONTHS.between(birthDate, today);
    }

    private void appendHealthContext(Map<String, Object> profileContext, HealthRecord healthRecord) {
        profileContext.put("healthRecordDate", healthRecord.getRecordDate());
        profileContext.put("heightCm", healthRecord.getHeight());
        profileContext.put("weightKg", healthRecord.getWeight());
        profileContext.put("bmi", healthRecord.getBmi());
        profileContext.put("activityLevel", healthRecord.getActivityLevel());
        profileContext.put("bmrKcal", healthRecord.getBmr());
        profileContext.put("tdeeKcal", healthRecord.getTdee());
        profileContext.put("tdeeFormula", healthRecord.getTdeeFormula());
    }

    private void appendRecentIllnessContext(Map<String, Object> profileContext, Profile profile) {
        List<IllnessEvent> events = illnessEventRepository
                .findTop3ByProfileIdOrderByStartAtDescIdDesc(profile.getId());
        if (events.isEmpty()) {
            return;
        }

        String illnessHistory = events.stream()
                .map(event -> {
                    String endDate = event.getEndAt() != null ? event.getEndAt().toString() : "đang theo dõi";
                    String description = sanitizeCatalogValue(event.getDescription());
                    return event.getStartAt() + " đến " + endDate
                            + ": " + sanitizeCatalogValue(event.getIllnessType())
                            + (description.isBlank() ? "" : " (" + description + ")");
                })
                .collect(Collectors.joining(" ;; "));
        profileContext.put("profileIllnessHistory", illnessHistory);
        profileContext.put("profileIllnessCount", events.size());
    }

    private void appendPreviousHealthContext(
            Map<String, Object> profileContext,
            HealthRecord latest,
            HealthRecord previous
    ) {
        profileContext.put("previousHealthRecordDate", previous.getRecordDate());
        profileContext.put("previousHeightCm", previous.getHeight());
        profileContext.put("previousWeightKg", previous.getWeight());
        profileContext.put("previousBmi", previous.getBmi());

        if (latest.getWeight() != null && previous.getWeight() != null) {
            profileContext.put("weightChangeKg", roundToOneDecimal(latest.getWeight() - previous.getWeight()));
        }
        if (latest.getHeight() != null && previous.getHeight() != null) {
            profileContext.put("heightChangeCm", roundToOneDecimal(latest.getHeight() - previous.getHeight()));
        }
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }

    private List<AgenticRagChatHistoryItem> buildChatHistory(AiChatConversation conversation) {
        List<AiChatMessage> latestMessages = messageRepository.findTop6ByConversationIdAndStatusOrderByCreatedAtDesc(
                conversation.getId(),
                Constants.TABLE_STATUS.ACTIVE
        );
        Collections.reverse(latestMessages);
        return latestMessages.stream()
                .map(message -> AgenticRagChatHistoryItem.builder()
                        .sender(message.getSender())
                        .message(message.getMessage())
                        .build())
                .toList();
    }

    private String buildTitle(String message) {
        String title = message.trim();
        if (title.length() <= 80) {
            return title;
        }
        return title.substring(0, 80);
    }

    private String writeUsedDomains(List<String> usedDomains) {
        if (usedDomains == null || usedDomains.isEmpty()) {
            return null;
        }
        return usedDomains.stream()
                .filter(domain -> domain != null && !domain.isBlank())
                .collect(Collectors.joining(","));
    }

    private String writeMap(Map<String, Object> value) {
        return value == null || value.isEmpty() ? null : String.valueOf(value);
    }

    private List<String> readUsedDomains(String usedDomains) {
        if (usedDomains == null || usedDomains.isBlank()) {
            return List.of();
        }
        return Arrays.stream(usedDomains.split(","))
                .map(String::trim)
                .filter(domain -> !domain.isBlank())
                .toList();
    }

    private AiChatConversationResponse mapConversation(AiChatConversation conversation) {
        return AiChatConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .profileId(conversation.getProfile() != null ? conversation.getProfile().getId() : null)
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private AiChatMessageResponse mapMessage(AiChatMessage message) {
        return AiChatMessageResponse.builder()
                .id(message.getId())
                .sender(message.getSender())
                .message(message.getMessage())
                .selectedAgent(message.getSelectedAgent())
                .intent(message.getIntent())
                .safetyLevel(message.getSafetyLevel())
                .usedDomains(readUsedDomains(message.getUsedDomains()))
                .createdAt(message.getCreatedAt())
                .build();
    }
}
