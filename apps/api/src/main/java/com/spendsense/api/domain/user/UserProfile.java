package com.spendsense.api.domain.user;

import com.spendsense.api.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {
    @Column(name = "supabase_user_id", nullable = false, unique = true, updatable = false)
    private UUID supabaseUserId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    protected UserProfile() {
    }

    public UserProfile(UUID supabaseUserId, String email) {
        setId(UUID.randomUUID());
        this.supabaseUserId = supabaseUserId;
        this.email = email;
    }

    public UUID getSupabaseUserId() {
        return supabaseUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public void refreshFromAuth(String email) {
        this.email = email;
        this.lastSeenAt = Instant.now();
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
        this.onboardingCompletedAt = Instant.now();
    }
}
