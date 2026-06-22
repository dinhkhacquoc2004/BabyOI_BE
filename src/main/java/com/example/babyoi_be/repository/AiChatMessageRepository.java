package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findByConversationConversationIdAndConversationUserIdAndStatusOrderByCreatedAtAsc(
            String conversationId,
            Long userId,
            Long status
    );

    List<AiChatMessage> findByConversationIdAndStatus(Long conversationId, Long status);

    List<AiChatMessage> findTop6ByConversationIdAndStatusOrderByCreatedAtDesc(Long conversationId, Long status);
}
