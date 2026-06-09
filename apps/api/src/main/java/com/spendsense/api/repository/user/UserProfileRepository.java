package com.spendsense.api.repository.user;

import com.spendsense.api.domain.user.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findBySupabaseUserId(UUID supabaseUserId);
}
