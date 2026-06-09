package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.ImportFailure;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportFailureRepository extends JpaRepository<ImportFailure, UUID> {
    List<ImportFailure> findByImportJobIdOrderByRowNumberAsc(UUID importJobId);
}
