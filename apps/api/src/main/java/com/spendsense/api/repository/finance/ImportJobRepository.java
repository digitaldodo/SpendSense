package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.ImportJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
    @EntityGraph(attributePaths = {"account"})
    List<ImportJob> findTop20ByUserProfileIdOrderByStartedAtDesc(UUID userProfileId);

    @EntityGraph(attributePaths = {"account"})
    Optional<ImportJob> findByIdAndUserProfileId(UUID id, UUID userProfileId);

    Optional<ImportJob> findByUserProfileIdAndIdempotencyKey(UUID userProfileId, String idempotencyKey);
}
