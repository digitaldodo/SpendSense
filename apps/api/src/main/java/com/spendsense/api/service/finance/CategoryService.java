package com.spendsense.api.service.finance;

import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.dto.finance.CategoryResponse;
import com.spendsense.api.mapper.finance.CategoryMapper;
import com.spendsense.api.repository.finance.CategoryRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final UserProfileSyncService userProfileSyncService;

    public CategoryService(
            CategoryRepository categoryRepository,
            CategoryMapper categoryMapper,
            UserProfileSyncService userProfileSyncService
    ) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.userProfileSyncService = userProfileSyncService;
    }

    @Transactional
    public List<CategoryResponse> listCategories(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        ensureSystemCategories();
        return categoryRepository.findByUserProfileIdIsNullOrUserProfileIdOrderBySortOrderAscNameAsc(userProfileId)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public void ensureSystemCategories() {
        for (SystemCategorySeed seed : SystemCategorySeed.defaults()) {
            categoryRepository.findBySlugAndUserProfileIdIsNull(seed.slug())
                    .orElseGet(() -> categoryRepository.save(new Category(
                            null,
                            seed.name(),
                            seed.slug(),
                            seed.colorToken(),
                            seed.iconName(),
                            true,
                            seed.sortOrder()
                    )));
        }
    }

    record SystemCategorySeed(String name, String slug, String colorToken, String iconName, int sortOrder) {
        static List<SystemCategorySeed> defaults() {
            return List.of(
                    new SystemCategorySeed("Food & Dining", "food-dining", "mint", "utensils", 10),
                    new SystemCategorySeed("Transport", "transport", "blue", "car", 20),
                    new SystemCategorySeed("Shopping", "shopping", "amber", "shopping-bag", 30),
                    new SystemCategorySeed("Income", "income", "green", "wallet", 40),
                    new SystemCategorySeed("Transfers", "transfers", "slate", "arrow-left-right", 50),
                    new SystemCategorySeed("Bills", "bills", "rose", "receipt", 60),
                    new SystemCategorySeed("Health", "health", "teal", "heart-pulse", 70),
                    new SystemCategorySeed("Other", "other", "neutral", "circle", 100)
            );
        }
    }
}
