package com.spendsense.api.service.profile;

import com.spendsense.api.domain.profile.FinancialPreferences;
import com.spendsense.api.domain.profile.FinancialGoal;
import com.spendsense.api.domain.profile.OnboardingProgress;
import com.spendsense.api.domain.profile.OnboardingStep;
import com.spendsense.api.domain.profile.SpendingHabit;
import com.spendsense.api.domain.user.UserProfile;
import com.spendsense.api.dto.profile.FinancialPreferencesResponse;
import com.spendsense.api.dto.profile.OnboardingProgressResponse;
import com.spendsense.api.dto.profile.OnboardingProgressUpdateRequest;
import com.spendsense.api.dto.profile.OnboardingStatusResponse;
import com.spendsense.api.dto.profile.ProfileResponse;
import com.spendsense.api.dto.profile.ProfileUpdateRequest;
import com.spendsense.api.exception.OnboardingIncompleteException;
import com.spendsense.api.repository.profile.FinancialPreferencesRepository;
import com.spendsense.api.repository.profile.OnboardingProgressRepository;
import com.spendsense.api.repository.user.UserProfileRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserProfileRepository userProfileRepository;
    private final OnboardingProgressRepository onboardingProgressRepository;
    private final FinancialPreferencesRepository financialPreferencesRepository;
    private final UserProfileSyncService userProfileSyncService;

    public ProfileService(
            UserProfileRepository userProfileRepository,
            OnboardingProgressRepository onboardingProgressRepository,
            FinancialPreferencesRepository financialPreferencesRepository,
            UserProfileSyncService userProfileSyncService
    ) {
        this.userProfileRepository = userProfileRepository;
        this.onboardingProgressRepository = onboardingProgressRepository;
        this.financialPreferencesRepository = financialPreferencesRepository;
        this.userProfileSyncService = userProfileSyncService;
    }

    @Transactional
    public ProfileResponse currentProfile(SupabasePrincipal principal) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        OnboardingProgress progress = getOrCreateProgress(profile);
        FinancialPreferences preferences = getOrCreatePreferences(profile);
        return toProfileResponse(profile, progress, preferences);
    }

    @Transactional
    public ProfileResponse updateProfile(SupabasePrincipal principal, ProfileUpdateRequest request) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        profile.updateDisplayName(trimToNull(request.displayName()));
        UserProfile savedProfile = userProfileRepository.save(profile);
        return toProfileResponse(savedProfile, getOrCreateProgress(savedProfile), getOrCreatePreferences(savedProfile));
    }

    @Transactional
    public ProfileResponse saveOnboardingProgress(
            SupabasePrincipal principal,
            OnboardingProgressUpdateRequest request
    ) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        OnboardingProgress progress = getOrCreateProgress(profile);
        FinancialPreferences preferences = getOrCreatePreferences(profile);

        int currentStep = request.currentStep() == null ? progress.getCurrentStep() : request.currentStep();
        Set<OnboardingStep> completedSteps = request.completedSteps() == null
                ? progress.getCompletedSteps()
                : request.completedSteps();
        progress.update(currentStep, new LinkedHashSet<>(completedSteps));

        if (request.salaryRange() != null) {
            preferences.updateSalaryRange(request.salaryRange());
        }
        if (request.employmentType() != null) {
            preferences.updateEmploymentType(request.employmentType());
        }
        if (request.monthlyFixedExpenses() != null) {
            preferences.updateMonthlyFixedExpenses(request.monthlyFixedExpenses());
        }
        if (request.goals() != null) {
            preferences.updateGoals(new LinkedHashSet<>(request.goals()));
        }
        if (request.spendingHabits() != null) {
            preferences.updateSpendingHabits(new LinkedHashSet<>(request.spendingHabits()));
        }
        if (request.riskComfort() != null) {
            preferences.updateRiskComfort(request.riskComfort());
        }

        OnboardingProgress savedProgress = onboardingProgressRepository.save(progress);
        FinancialPreferences savedPreferences = financialPreferencesRepository.save(preferences);
        return toProfileResponse(profile, savedProgress, savedPreferences);
    }

    @Transactional
    public ProfileResponse completeOnboarding(SupabasePrincipal principal) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        OnboardingProgress progress = getOrCreateProgress(profile);
        FinancialPreferences preferences = getOrCreatePreferences(profile);

        if (!preferences.isCompletionReady()) {
            throw new OnboardingIncompleteException("Please finish each onboarding step before continuing.");
        }

        progress.complete();
        profile.completeOnboarding();

        UserProfile savedProfile = userProfileRepository.save(profile);
        OnboardingProgress savedProgress = onboardingProgressRepository.save(progress);
        FinancialPreferences savedPreferences = financialPreferencesRepository.save(preferences);
        return toProfileResponse(savedProfile, savedProgress, savedPreferences);
    }

    @Transactional
    public OnboardingStatusResponse onboardingStatus(SupabasePrincipal principal) {
        UserProfile profile = userProfileRepository.findBySupabaseUserId(principal.id())
                .orElseGet(() -> userProfileSyncService.syncAuthenticatedUser(principal));
        OnboardingProgress progress = onboardingProgressRepository.findByUserProfileId(profile.getId())
                .orElse(new OnboardingProgress(profile));
        return new OnboardingStatusResponse(
                profile.isOnboardingCompleted(),
                progress.getCurrentStep(),
                Set.copyOf(progress.getCompletedSteps()),
                profile.isOnboardingCompleted() ? "/dashboard" : "/onboarding"
        );
    }

    @Transactional(readOnly = true)
    public boolean isOnboardingComplete(UUID supabaseUserId) {
        return userProfileRepository.findBySupabaseUserId(supabaseUserId)
                .map(UserProfile::isOnboardingCompleted)
                .orElse(false);
    }

    private OnboardingProgress getOrCreateProgress(UserProfile profile) {
        return onboardingProgressRepository.findByUserProfileId(profile.getId())
                .orElseGet(() -> onboardingProgressRepository.save(new OnboardingProgress(profile)));
    }

    private FinancialPreferences getOrCreatePreferences(UserProfile profile) {
        return financialPreferencesRepository.findByUserProfileId(profile.getId())
                .orElseGet(() -> financialPreferencesRepository.save(new FinancialPreferences(profile)));
    }

    private ProfileResponse toProfileResponse(
            UserProfile profile,
            OnboardingProgress progress,
            FinancialPreferences preferences
    ) {
        return new ProfileResponse(
                profile.getId(),
                profile.getSupabaseUserId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.isOnboardingCompleted(),
                profile.getOnboardingCompletedAt(),
                new OnboardingProgressResponse(
                        progress.getCurrentStep(),
                        Set.copyOf(progress.getCompletedSteps()),
                        progress.getCompletedAt()
                ),
                new FinancialPreferencesResponse(
                        preferences.getSalaryRange(),
                        preferences.getEmploymentType(),
                        preferences.getMonthlyFixedExpenses(),
                        Set.copyOf(preferences.getGoals()),
                        Set.copyOf(preferences.getSpendingHabits()),
                        preferences.getRiskComfort()
                ),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
