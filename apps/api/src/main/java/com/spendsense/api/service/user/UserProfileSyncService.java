package com.spendsense.api.service.user;

import com.spendsense.api.domain.user.UserProfile;
import com.spendsense.api.repository.user.UserProfileRepository;
import com.spendsense.api.security.SupabasePrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileSyncService {
    private final UserProfileRepository userProfileRepository;

    public UserProfileSyncService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile syncAuthenticatedUser(SupabasePrincipal principal) {
        UserProfile profile = userProfileRepository.findBySupabaseUserId(principal.id())
                .orElseGet(() -> new UserProfile(principal.id(), principal.email()));
        profile.refreshFromAuth(principal.email());
        return userProfileRepository.save(profile);
    }
}
