package com.example.babyoi_be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiChatConversation conversation;

    @Column(name = "sender", nullable = false, length = 20)
    private String sender;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "selected_agent", length = 100)
    private String selectedAgent;

    @Column(name = "intent", length = 100)
    private String intent;

    @Column(name = "safety_level", length = 100)
    private String safetyLevel;

    @Column(name = "used_domains", columnDefinition = "TEXT")
    private String usedDomains;

    @Column(name = "debug_log", columnDefinition = "TEXT")
    private String debugLog;

    @Column(name = "raw_result", columnDefinition = "TEXT")
    private String rawResult;

    @Column(name = "status")
    private Long status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = 2L;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
