package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.domain.finance.CustomCategoryAudit;
import com.spendsense.api.domain.user.UserProfile;
import com.spendsense.api.dto.finance.CategoryMergeRequest;
import com.spendsense.api.dto.finance.CategoryRequest;
import com.spendsense.api.dto.finance.CategoryResponse;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.mapper.finance.CategoryMapper;
import com.spendsense.api.repository.finance.BudgetRepository;
import com.spendsense.api.repository.finance.CategoryRepository;
import com.spendsense.api.repository.finance.CustomCategoryAuditRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CustomCategoryAuditRepository customCategoryAuditRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryMapper categoryMapper;
    private final UserProfileSyncService userProfileSyncService;
    private final ObjectMapper objectMapper;

    public CategoryService(
            CategoryRepository categoryRepository,
            CustomCategoryAuditRepository customCategoryAuditRepository,
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            CategoryMapper categoryMapper,
            UserProfileSyncService userProfileSyncService,
            ObjectMapper objectMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.customCategoryAuditRepository = customCategoryAuditRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.categoryMapper = categoryMapper;
        this.userProfileSyncService = userProfileSyncService;
        this.objectMapper = objectMapper;
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
    public CategoryResponse createCategory(SupabasePrincipal principal, CategoryRequest request) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        String name = request.name().trim();
        Category category = categoryRepository.save(new Category(
                profile,
                name,
                uniqueSlug(slugify(name), profile.getId()),
                defaulted(request.colorToken(), "green"),
                defaulted(request.iconName(), "tag"),
                false,
                90
        ));
        customCategoryAuditRepository.save(new CustomCategoryAudit(
                profile,
                category,
                "CREATED",
                null,
                writeJson(snapshot(category)),
                request.reason()
        ));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(SupabasePrincipal principal, UUID categoryId, CategoryRequest request) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Category category = categoryRepository.findVisibleById(categoryId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        if (category.isSystemCategory() || category.getUserProfile() == null) {
            throw new IllegalArgumentException("System categories cannot be renamed. Create a custom category instead.");
        }
        String beforeJson = writeJson(snapshot(category));
        String name = request.name().trim();
        category.updateDetails(
                name,
                category.getName().equals(name) ? category.getSlug() : uniqueSlug(slugify(name), profile.getId()),
                defaulted(request.colorToken(), category.getColorToken()),
                defaulted(request.iconName(), category.getIconName())
        );
        Category saved = categoryRepository.save(category);
        customCategoryAuditRepository.save(new CustomCategoryAudit(
                profile,
                saved,
                "UPDATED",
                beforeJson,
                writeJson(snapshot(saved)),
                request.reason()
        ));
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse mergeCategory(SupabasePrincipal principal, UUID sourceCategoryId, CategoryMergeRequest request) {
        UserProfile profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Category source = categoryRepository.findVisibleById(sourceCategoryId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Source category not found."));
        Category target = categoryRepository.findVisibleById(request.targetCategoryId(), profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Target category not found."));
        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Choose two different categories to merge.");
        }
        if (source.isSystemCategory() || source.getUserProfile() == null) {
            throw new IllegalArgumentException("System categories cannot be merged away.");
        }
        String beforeJson = writeJson(Map.of("source", snapshot(source), "target", snapshot(target)));
        int movedTransactions = transactionRepository.moveTransactionsToCategory(profile.getId(), source.getId(), target);
        int movedBudgets = budgetRepository.moveBudgetsToCategory(profile.getId(), source.getId(), target);
        categoryRepository.delete(source);
        customCategoryAuditRepository.save(new CustomCategoryAudit(
                profile,
                target,
                "MERGED",
                beforeJson,
                writeJson(Map.of(
                        "target", snapshot(target),
                        "movedTransactions", movedTransactions,
                        "movedBudgets", movedBudgets
                )),
                request.reason()
        ));
        return categoryMapper.toResponse(target);
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

    private String uniqueSlug(String baseSlug, UUID userProfileId) {
        String slug = baseSlug.isBlank() ? "category" : baseSlug;
        String candidate = slug;
        int suffix = 2;
        while (categoryRepository.findBySlugAndUserProfileId(candidate, userProfileId).isPresent()
                || categoryRepository.findBySlugAndUserProfileIdIsNull(candidate).isPresent()) {
            candidate = slug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String defaulted(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Map<String, Object> snapshot(Category category) {
        return Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "slug", category.getSlug(),
                "colorToken", category.getColorToken(),
                "iconName", category.getIconName(),
                "systemCategory", category.isSystemCategory()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write category audit metadata.", exception);
        }
    }
}
