package com.spendsense.api.mapper.finance;

import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.dto.finance.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getColorToken(),
                category.getIconName(),
                category.isSystemCategory()
        );
    }
}
