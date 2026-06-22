package com.example.babyoi_be.repository;

import com.example.babyoi_be.domain.entity.AiChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiChatConversationRepository extends JpaRepository<AiChatConversation, Long> {
    Optional<AiChatConversation> findByConversationIdAndStatus(String conversationId, Long status);

    List<AiChatConversation> findByUserIdAndStatusOrderByUpdatedAtDescCreatedAtDescIdDesc(Long userId, Long status);

    List<AiChatConversation> findByProfileIdAndUserIdAndStatusOrderByUpdatedAtDesc(
            Long profileId,
            Long userId,
            Long status
    );
}
