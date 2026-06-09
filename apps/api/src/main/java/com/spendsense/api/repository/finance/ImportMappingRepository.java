package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.ImportMapping;
import com.spendsense.api.domain.finance.IngestionSource;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportMappingRepository extends JpaRepository<ImportMapping, UUID> {
    Optional<ImportMapping> findByUserProfileIdAndSourceAndFileSignature(
            UUID userProfileId,
            IngestionSource source,
            String fileSignature
    );
}
