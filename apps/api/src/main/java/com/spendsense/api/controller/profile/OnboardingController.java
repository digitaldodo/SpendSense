package com.spendsense.api.controller.profile;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.profile.OnboardingProgressUpdateRequest;
import com.spendsense.api.dto.profile.OnboardingStatusResponse;
import com.spendsense.api.dto.profile.ProfileResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.profile.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {
    private final ProfileService profileService;

    public OnboardingController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/status")
    ResponseEntity<ApiResponse<OnboardingStatusResponse>> status(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.onboardingStatus(principal),
                "Onboarding status loaded.",
                traceId
        ));
    }

    @PatchMapping("/progress")
    ResponseEntity<ApiResponse<ProfileResponse>> saveProgress(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody OnboardingProgressUpdateRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.saveOnboardingProgress(principal, request),
                "Onboarding progress saved.",
                traceId
        ));
    }

    @PostMapping("/complete")
    ResponseEntity<ApiResponse<ProfileResponse>> complete(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.completeOnboarding(principal),
                "Onboarding completed.",
                traceId
        ));
    }
}
