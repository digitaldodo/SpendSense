package com.spendsense.api.service.delivery;

import com.spendsense.api.dto.engagement.EmailPreviewResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeliveryMonitoringService {
    private final UserProfileSyncService userProfileSyncService;
    private final DigestGenerationService digestGenerationService;

    public DeliveryMonitoringService(
            UserProfileSyncService userProfileSyncService,
            DigestGenerationService digestGenerationService
    ) {
        this.userProfileSyncService = userProfileSyncService;
        this.digestGenerationService = digestGenerationService;
    }

    public EmailPreviewResponse preview(SupabasePrincipal principal, String templateType) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        EmailTemplate template = digestGenerationService.preview(userProfileId, templateType);
        return new EmailPreviewResponse(template.templateType(), template.subject(), template.html(), template.text());
    }
}
