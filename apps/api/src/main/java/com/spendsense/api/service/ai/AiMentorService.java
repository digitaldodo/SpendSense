package com.spendsense.api.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.config.SpendSenseProperties;
import com.spendsense.api.dto.ai.AiChatRequest;
import com.spendsense.api.dto.ai.AiChatResponse;
import com.spendsense.api.dto.ai.AiConversationDetailResponse;
import com.spendsense.api.dto.ai.AiConversationSummaryResponse;
import com.spendsense.api.dto.ai.AiFeedbackRequest;
import com.spendsense.api.dto.ai.AiFeedbackResponse;
import com.spendsense.api.dto.ai.AiInsightCardResponse;
import com.spendsense.api.dto.ai.AiMessageResponse;
import com.spendsense.api.dto.ai.AiUsageResponse;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiMentorService {
    private static final TypeReference<List<AiInsightCardResponse>> INSIGHT_CARD_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final UserProfileSyncService userProfileSyncService;
    private final AiSafetyService aiSafetyService;
    private final AiFinancialContextBuilder contextBuilder;
    private final AiPromptOrchestrator promptOrchestrator;
    private final AiProvider aiProvider;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SpendSenseProperties properties;

    public AiMentorService(
            UserProfileSyncService userProfileSyncService,
            AiSafetyService aiSafetyService,
            AiFinancialContextBuilder contextBuilder,
            AiPromptOrchestrator promptOrchestrator,
            AiProvider aiProvider,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            SpendSenseProperties properties
    ) {
        this.userProfileSyncService = userProfileSyncService;
        this.aiSafetyService = aiSafetyService;
        this.contextBuilder = contextBuilder;
        this.promptOrchestrator = promptOrchestrator;
        this.aiProvider = aiProvider;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional
    public List<AiConversationSummaryResponse> listConversations(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return jdbcTemplate.query("""
                select c.id, c.title, c.status, c.context_scope, c.last_message_at, c.created_at,
                       coalesce((
                           select m.content
                           from ai_messages m
                           where m.conversation_id = c.id
                           order by m.created_at desc
                           limit 1
                       ), '') as last_message_preview
                from ai_conversations c
                where c.user_profile_id = ?
                order by c.last_message_at desc
                limit 25
                """, this::conversationRow, userProfileId);
    }

    @Transactional
    public AiConversationDetailResponse getConversation(SupabasePrincipal principal, UUID conversationId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        AiConversationSummaryResponse conversation = findConversation(userProfileId, conversationId);
        List<AiMessageResponse> messages = jdbcTemplate.query("""
                select id, conversation_id, role, intent, content, structured_json, safety_flags,
                       provider, model, prompt_tokens, completion_tokens, latency_ms, created_at
                from ai_messages
                where user_profile_id = ? and conversation_id = ?
                order by created_at asc
                """, this::messageRow, userProfileId, conversationId);
        return new AiConversationDetailResponse(conversation, messages);
    }

    @Transactional
    public AiChatResponse chat(SupabasePrincipal principal, AiChatRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        AiSafetyResult safety = aiSafetyService.inspect(promptFrom(request), request.intent());
        UUID conversationId = request.conversationId() == null
                ? createConversation(userProfileId, safety.sanitizedPrompt(), safety.intent())
                : ensureConversation(userProfileId, request.conversationId());
        AiFinancialContext context = contextBuilder.build(userProfileId, maxContextItems());
        UUID userMessageId = persistMessage(
                userProfileId,
                conversationId,
                "USER",
                safety.intent(),
                safety.sanitizedPrompt(),
                "{}",
                writeJson(requestSourceMetadata(request)),
                writeJson(safety.flags()),
                null,
                null,
                0,
                0,
                0
        );
        AiProviderResult providerResult = aiProvider.generate(new AiProviderRequest(
                promptOrchestrator.systemPrompt(),
                safety.sanitizedPrompt(),
                safety.intent(),
                context,
                safety.flags()
        ));
        String structuredJson = writeJson(Map.of(
                "insightCards", providerResult.insightCards(),
                "followUpPrompts", providerResult.followUpPrompts(),
                "citations", providerResult.citations(),
                "grounded", true
        ));
        UUID assistantMessageId = persistMessage(
                userProfileId,
                conversationId,
                "ASSISTANT",
                safety.intent(),
                providerResult.content(),
                structuredJson,
                writeJson(safeContextSnapshot(context)),
                writeJson(safety.flags()),
                providerResult.provider(),
                providerResult.model(),
                providerResult.promptTokens(),
                providerResult.completionTokens(),
                providerResult.latencyMs()
        );
        persistUsage(userProfileId, conversationId, assistantMessageId, providerResult, safety);
        jdbcTemplate.update("update ai_conversations set last_message_at = current_timestamp, updated_at = current_timestamp where id = ?", conversationId);

        AiConversationSummaryResponse conversation = findConversation(userProfileId, conversationId);
        AiMessageResponse userMessage = getMessage(userProfileId, userMessageId);
        AiMessageResponse assistantMessage = getMessage(userProfileId, assistantMessageId);
        AiUsageResponse usage = new AiUsageResponse(
                providerResult.provider(),
                providerResult.model(),
                providerResult.promptTokens(),
                providerResult.completionTokens(),
                providerResult.promptTokens() + providerResult.completionTokens(),
                providerResult.estimatedCostMinor(),
                costCurrency(),
                providerResult.latencyMs()
        );
        return new AiChatResponse(conversation, userMessage, assistantMessage, providerResult.insightCards(), providerResult.followUpPrompts(), usage, true, safety.safetyLevel(), providerResult.citations());
    }

    @Transactional
    public List<AiInsightCardResponse> insightTimeline(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        AiFinancialContext context = contextBuilder.build(userProfileId, maxContextItems());
        AiProviderResult result = aiProvider.generate(new AiProviderRequest(promptOrchestrator.systemPrompt(), "Build timeline cards", "GENERAL_FINANCIAL_SUMMARY", context, List.of()));
        return result.insightCards();
    }

    @Transactional
    public AiFeedbackResponse feedback(SupabasePrincipal principal, UUID messageId, AiFeedbackRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        AiMessageResponse message = getMessage(userProfileId, messageId);
        UUID feedbackId = UUID.randomUUID();
        String feedbackType = request.feedbackType() == null || request.feedbackType().isBlank() ? "GENERAL" : request.feedbackType();
        jdbcTemplate.update("""
                insert into ai_feedback (
                    id, user_profile_id, conversation_id, message_id, rating, feedback_type, comment,
                    metadata_json, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                feedbackId,
                userProfileId,
                message.conversationId(),
                messageId,
                request.rating(),
                feedbackType,
                request.comment(),
                "{}"
        );
        return new AiFeedbackResponse(feedbackId, messageId, request.rating(), feedbackType, Instant.now());
    }

    private String promptFrom(AiChatRequest request) {
        if (request.prompt() != null && !request.prompt().isBlank()) {
            return request.prompt();
        }
        if (request.intent() != null && !request.intent().isBlank()) {
            return switch (request.intent()) {
                case "OVERSPEND_EXPLANATION" -> "Why did I overspend this month?";
                case "EMI_SAFETY" -> "How safe is this EMI?";
                case "CATEGORY_SAVINGS_IMPACT" -> "What category hurts my savings most?";
                case "HEALTH_SCORE_GUIDANCE" -> "How can I improve my financial health score?";
                case "MONTHLY_CHANGE" -> "What changed compared to last month?";
                case "RECOMMENDATION_EXPLANATION" -> "Explain the recommendation in my action center.";
                case "HABIT_COACHING" -> "Explain my current financial habit momentum.";
                case "WEEKLY_RECAP" -> "Give me a grounded weekly recap.";
                default -> "Summarize my financial picture.";
            };
        }
        return "Summarize my financial picture.";
    }

    private UUID createConversation(UUID userProfileId, String prompt, String intent) {
        UUID id = UUID.randomUUID();
        String title = titleFor(prompt, intent);
        jdbcTemplate.update("""
                insert into ai_conversations (
                    id, user_profile_id, title, status, context_scope, last_message_at, created_at, updated_at
                ) values (?, ?, ?, 'ACTIVE', 'FINANCIAL_WORKSPACE', current_timestamp, current_timestamp, current_timestamp)
                """, id, userProfileId, title);
        return id;
    }

    private UUID ensureConversation(UUID userProfileId, UUID conversationId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from ai_conversations where id = ? and user_profile_id = ?", Integer.class, conversationId, userProfileId);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("AI conversation not found.");
        }
        return conversationId;
    }

    private AiConversationSummaryResponse findConversation(UUID userProfileId, UUID conversationId) {
        return jdbcTemplate.query("""
                select c.id, c.title, c.status, c.context_scope, c.last_message_at, c.created_at,
                       coalesce((
                           select m.content
                           from ai_messages m
                           where m.conversation_id = c.id
                           order by m.created_at desc
                           limit 1
                       ), '') as last_message_preview
                from ai_conversations c
                where c.user_profile_id = ? and c.id = ?
                """, this::conversationRow, userProfileId, conversationId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("AI conversation not found."));
    }

    private AiMessageResponse getMessage(UUID userProfileId, UUID messageId) {
        return jdbcTemplate.query("""
                select id, conversation_id, role, intent, content, structured_json, safety_flags,
                       provider, model, prompt_tokens, completion_tokens, latency_ms, created_at
                from ai_messages
                where user_profile_id = ? and id = ?
                """, this::messageRow, userProfileId, messageId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("AI message not found."));
    }

    private UUID persistMessage(
            UUID userProfileId,
            UUID conversationId,
            String role,
            String intent,
            String content,
            String structuredJson,
            String groundedContextJson,
            String safetyFlags,
            String provider,
            String model,
            int promptTokens,
            int completionTokens,
            int latencyMs
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into ai_messages (
                    id, conversation_id, user_profile_id, role, intent, content, structured_json,
                    grounded_context_json, safety_flags, provider, model, prompt_tokens,
                    completion_tokens, latency_ms, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                """,
                id,
                conversationId,
                userProfileId,
                role,
                intent,
                content,
                structuredJson,
                groundedContextJson,
                safetyFlags,
                provider,
                model,
                promptTokens,
                completionTokens,
                latencyMs
        );
        return id;
    }

    private void persistUsage(UUID userProfileId, UUID conversationId, UUID messageId, AiProviderResult result, AiSafetyResult safety) {
        jdbcTemplate.update("""
                insert into ai_usage_logs (
                    id, user_profile_id, conversation_id, message_id, provider, model, prompt_tokens,
                    completion_tokens, total_tokens, estimated_cost_minor, currency, status,
                    safety_outcome, latency_ms, metadata_json, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                """,
                UUID.randomUUID(),
                userProfileId,
                conversationId,
                messageId,
                result.provider(),
                result.model(),
                result.promptTokens(),
                result.completionTokens(),
                result.promptTokens() + result.completionTokens(),
                result.estimatedCostMinor(),
                costCurrency(),
                safety.blocked() ? "GUARDED" : "SUCCESS",
                safety.safetyLevel(),
                result.latencyMs(),
                writeJson(Map.of("flags", safety.flags(), "deterministic", true))
        );
    }

    private AiConversationSummaryResponse conversationRow(ResultSet rs, int rowNum) throws SQLException {
        String preview = rs.getString("last_message_preview");
        return new AiConversationSummaryResponse(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("status"),
                rs.getString("context_scope"),
                instant(rs, "last_message_at"),
                instant(rs, "created_at"),
                preview == null || preview.length() <= 120 ? preview : preview.substring(0, 120)
        );
    }

    private AiMessageResponse messageRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> structured = readMap(rs.getString("structured_json"));
        List<AiInsightCardResponse> cards = convert(structured.get("insightCards"), INSIGHT_CARD_LIST);
        List<String> followUps = convert(structured.get("followUpPrompts"), STRING_LIST);
        List<String> flags = readList(rs.getString("safety_flags"));
        return new AiMessageResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("conversation_id", UUID.class),
                rs.getString("role"),
                rs.getString("intent"),
                rs.getString("content"),
                cards,
                followUps,
                flags,
                rs.getString("provider"),
                rs.getString("model"),
                rs.getInt("prompt_tokens"),
                rs.getInt("completion_tokens"),
                rs.getInt("latency_ms"),
                instant(rs, "created_at")
        );
    }

    private Map<String, Object> safeContextSnapshot(AiFinancialContext context) {
        return Map.of(
                "generatedAt", context.generatedAt(),
                "latestMonth", context.latestMonth(),
                "previousMonth", context.previousMonth(),
                "averageIncome", context.averageIncome(),
                "averageExpense", context.averageExpense(),
                "averageFreeCashflow", context.averageFreeCashflow(),
                "budgetsReviewed", context.budgets().size(),
                "goalsReviewed", context.goals().size(),
                "recentTransactionsReviewed", context.recentTransactions().size(),
                "notes", context.deterministicNotes()
        );
    }

    private Map<String, Object> requestSourceMetadata(AiChatRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (request.sourceTransactionId() != null) {
            metadata.put("sourceTransactionId", request.sourceTransactionId());
        }
        if (request.sourceBudgetId() != null) {
            metadata.put("sourceBudgetId", request.sourceBudgetId());
        }
        if (request.sourceGoalId() != null) {
            metadata.put("sourceGoalId", request.sourceGoalId());
        }
        return metadata;
    }

    private String titleFor(String prompt, String intent) {
        String value = prompt == null || prompt.isBlank() ? intent.replace('_', ' ') : prompt;
        value = value.replaceAll("\\s+", " ").trim();
        if (value.length() > 56) {
            value = value.substring(0, 56).trim();
        }
        return value.isBlank() ? "SpendSense mentor chat" : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write AI mentor metadata.", exception);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private List<String> readList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private <T> T convert(Object value, TypeReference<T> typeReference) {
        if (value == null) {
            return objectMapper.convertValue(List.of(), typeReference);
        }
        return objectMapper.convertValue(value, typeReference);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private int maxContextItems() {
        Integer configured = properties.ai() == null ? null : properties.ai().maxContextItems();
        return configured == null ? 12 : Math.max(4, Math.min(24, configured));
    }

    private String costCurrency() {
        if (properties.ai() == null || properties.ai().estimatedCostPerThousandTokens() == null || properties.ai().estimatedCostPerThousandTokens().currency() == null) {
            return "INR";
        }
        return properties.ai().estimatedCostPerThousandTokens().currency();
    }
}
