package com.spendsense.api.controller.delivery;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.delivery.WebhookIngestionResponse;
import com.spendsense.api.service.delivery.ProviderWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/providers")
public class ProviderWebhookController {
    private final ProviderWebhookService providerWebhookService;

    public ProviderWebhookController(ProviderWebhookService providerWebhookService) {
        this.providerWebhookService = providerWebhookService;
    }

    @PostMapping("/{provider}")
    ResponseEntity<ApiResponse<WebhookIngestionResponse>> ingest(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                providerWebhookService.ingest(provider, payload, headers, sourceIp(request)),
                "Provider webhook accepted.",
                traceId
        ));
    }

    private String sourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
