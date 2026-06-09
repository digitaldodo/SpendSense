package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserProfileIdIsNullOrUserProfileIdOrderBySortOrderAscNameAsc(UUID userProfileId);

    @Query("""
            select c
            from Category c
            where c.id = :id
              and (c.userProfile is null or c.userProfile.id = :userProfileId)
            """)
    Optional<Category> findVisibleById(UUID id, UUID userProfileId);

    Optional<Category> findBySlugAndUserProfileIdIsNull(String slug);
}
