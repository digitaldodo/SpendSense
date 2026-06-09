package com.spendsense.api.domain.profile;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "onboarding_progress")
public class OnboardingProgress extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    private UserProfile userProfile;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @ElementCollection(targetClass = OnboardingStep.class)
    @CollectionTable(
            name = "onboarding_completed_steps",
            joinColumns = @JoinColumn(name = "onboarding_progress_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false)
    private Set<OnboardingStep> completedSteps = new LinkedHashSet<>();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected OnboardingProgress() {
    }

    public OnboardingProgress(UserProfile userProfile) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public Set<OnboardingStep> getCompletedSteps() {
        return completedSteps;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void update(int currentStep, Set<OnboardingStep> completedSteps) {
        this.currentStep = currentStep;
        this.completedSteps.clear();
        this.completedSteps.addAll(completedSteps);
    }

    public void complete() {
        this.currentStep = 7;
        this.completedSteps.addAll(Set.of(OnboardingStep.values()));
        this.completedAt = Instant.now();
    }
}
