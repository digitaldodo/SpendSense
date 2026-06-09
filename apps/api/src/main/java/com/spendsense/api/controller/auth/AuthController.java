package com.spendsense.api.controller.auth;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.auth.AuthenticatedUserResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserProfileSyncService userProfileSyncService;

    public AuthController(UserProfileSyncService userProfileSyncService) {
        this.userProfileSyncService = userProfileSyncService;
    }

    @GetMapping("/me")
    ResponseEntity<ApiResponse<AuthenticatedUserResponse>> currentUser(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        userProfileSyncService.syncAuthenticatedUser(principal);
        AuthenticatedUserResponse response = new AuthenticatedUserResponse(
                principal.id(),
                principal.email(),
                principal.roles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()),
                Instant.now()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Authenticated session loaded.", traceId));
    }
}
