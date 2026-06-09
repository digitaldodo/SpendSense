package com.spendsense.api.controller.profile;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.profile.ProfileResponse;
import com.spendsense.api.dto.profile.ProfileUpdateRequest;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.profile.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/current")
    ResponseEntity<ApiResponse<ProfileResponse>> currentProfile(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.currentProfile(principal),
                "Profile loaded.",
                traceId
        ));
    }

    @PatchMapping
    ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.updateProfile(principal, request),
                "Profile updated.",
                traceId
        ));
    }
}
