package com.spendsense.api.domain.finance;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "slug", nullable = false, length = 140)
    private String slug;

    @Column(name = "color_token", nullable = false, length = 48)
    private String colorToken;

    @Column(name = "icon_name", nullable = false, length = 64)
    private String iconName;

    @Column(name = "system_category", nullable = false)
    private boolean systemCategory;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Category() {
    }

    public Category(
            UserProfile userProfile,
            String name,
            String slug,
            String colorToken,
            String iconName,
            boolean systemCategory,
            int sortOrder
    ) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.name = name;
        this.slug = slug;
        this.colorToken = colorToken;
        this.iconName = iconName;
        this.systemCategory = systemCategory;
        this.sortOrder = sortOrder;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getColorToken() {
        return colorToken;
    }

    public String getIconName() {
        return iconName;
    }

    public boolean isSystemCategory() {
        return systemCategory;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
