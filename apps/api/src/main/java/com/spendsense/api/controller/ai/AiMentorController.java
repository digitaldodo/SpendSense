package com.spendsense.api.controller.ai;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.ai.AiChatRequest;
import com.spendsense.api.dto.ai.AiChatResponse;
import com.spendsense.api.dto.ai.AiConversationDetailResponse;
import com.spendsense.api.dto.ai.AiConversationSummaryResponse;
import com.spendsense.api.dto.ai.AiFeedbackRequest;
import com.spendsense.api.dto.ai.AiFeedbackResponse;
import com.spendsense.api.dto.ai.AiInsightCardResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.ai.AiMentorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiMentorController {
    private final AiMentorService aiMentorService;

    public AiMentorController(AiMentorService aiMentorService) {
        this.aiMentorService = aiMentorService;
    }

    @GetMapping("/conversations")
    ResponseEntity<ApiResponse<List<AiConversationSummaryResponse>>> conversations(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiMentorService.listConversations(principal), "AI conversations loaded.", traceId));
    }

    @GetMapping("/conversations/{conversationId}")
    ResponseEntity<ApiResponse<AiConversationDetailResponse>> conversation(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID conversationId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiMentorService.getConversation(principal, conversationId), "AI conversation loaded.", traceId));
    }

    @PostMapping("/conversations/messages")
    ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody AiChatRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiMentorService.chat(principal, request), "SpendSense mentor response generated.", traceId));
    }

    @GetMapping("/insights/timeline")
    ResponseEntity<ApiResponse<List<AiInsightCardResponse>>> insightTimeline(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiMentorService.insightTimeline(principal), "AI insight timeline loaded.", traceId));
    }

    @PostMapping("/messages/{messageId}/feedback")
    ResponseEntity<ApiResponse<AiFeedbackResponse>> feedback(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID messageId,
            @Valid @RequestBody AiFeedbackRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiMentorService.feedback(principal, messageId, request), "AI feedback saved.", traceId));
    }
}
