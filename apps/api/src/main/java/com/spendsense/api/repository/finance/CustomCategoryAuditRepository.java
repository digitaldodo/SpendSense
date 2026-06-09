package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.CustomCategoryAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomCategoryAuditRepository extends JpaRepository<CustomCategoryAudit, UUID> {
}
