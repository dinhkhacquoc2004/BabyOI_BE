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
import com.example.babyoi_be.domain.entity.Profile;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.AiChatConversationRepository;
import com.example.babyoi_be.repository.AiChatMessageRepository;
import com.example.babyoi_be.repository.ProfileRepository;
import com.example.babyoi_be.security.CustomUserDetails;
import com.example.babyoi_be.service.AgenticRagClient;
import com.example.babyoi_be.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final String SENDER_USER = "USER";
    private static final String SENDER_ASSISTANT = "ASSISTANT";

    private final AiChatConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;
    private final ProfileRepository profileRepository;
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

        Map<String, Object> profileContext = buildProfileContext(profile);
        List<AgenticRagChatHistoryItem> chatHistory = buildChatHistory(conversation);

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

    private Map<String, Object> buildProfileContext(Profile profile) {
        if (profile == null) {
            return null;
        }
        Map<String, Object> profileContext = new LinkedHashMap<>();
        profileContext.put("id", profile.getId());
        profileContext.put("name", profile.getName());
        profileContext.put("dateOfBirth", profile.getDateOfBirth());
        profileContext.put("sex", profile.getSex() != null ? profile.getSex().name() : null);
        profileContext.put("profileType", profile.getProfileType());
        profileContext.put("profileCode", profile.getProfileCode());
        return profileContext;
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
