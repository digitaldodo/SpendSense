package com.spendsense.api.dto.profile;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID supabaseUserId,
        String email,
        String displayName,
        Set<String> roles,
        boolean onboardingCompleted,
        Instant onboardingCompletedAt,
        OnboardingProgressResponse onboardingProgress,
        FinancialPreferencesResponse financialPreferences,
        Instant createdAt,
        Instant updatedAt
) {
}
