package com.spendsense.api.service.ingestion;

import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.repository.finance.CategoryRepository;
import com.spendsense.api.service.finance.CategoryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CategoryAutoMappingService {
    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put("food-dining", List.of("swiggy", "zomato", "restaurant", "cafe", "food", "fresh", "instamart"));
        KEYWORDS.put("transport", List.of("uber", "ola", "metro", "irctc", "fuel", "petrol", "rapido"));
        KEYWORDS.put("shopping", List.of("amazon", "flipkart", "myntra", "nykaa", "shopping"));
        KEYWORDS.put("income", List.of("salary", "payroll", "interest", "dividend"));
        KEYWORDS.put("transfers", List.of("upi", "neft", "imps", "rtgs", "transfer"));
        KEYWORDS.put("bills", List.of("bill", "recharge", "airtel", "jio", "bescom", "electricity", "broadband"));
        KEYWORDS.put("health", List.of("pharmacy", "apollo", "hospital", "clinic", "medical"));
    }

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public CategoryAutoMappingService(CategoryRepository categoryRepository, CategoryService categoryService) {
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
    }

    public Category map(String merchantNormalized, String description) {
        categoryService.ensureSystemCategories();
        String searchable = ((merchantNormalized == null ? "" : merchantNormalized) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            if (entry.getValue().stream().anyMatch(searchable::contains)) {
                return systemCategory(entry.getKey());
            }
        }
        return systemCategory("other");
    }

    private Category systemCategory(String slug) {
        return categoryRepository.findBySlugAndUserProfileIdIsNull(slug).orElse(null);
    }
}
