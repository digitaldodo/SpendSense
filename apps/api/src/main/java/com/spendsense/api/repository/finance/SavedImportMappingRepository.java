package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.IngestionSource;
import com.spendsense.api.domain.finance.SavedImportMapping;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedImportMappingRepository extends JpaRepository<SavedImportMapping, UUID> {
    List<SavedImportMapping> findByUserProfileIdOrderByLastUsedAtDesc(UUID userProfileId);

    Optional<SavedImportMapping> findByIdAndUserProfileId(UUID id, UUID userProfileId);

    Optional<SavedImportMapping> findByUserProfileIdAndSourceAndFileSignature(
            UUID userProfileId,
            IngestionSource source,
            String fileSignature
    );
}
