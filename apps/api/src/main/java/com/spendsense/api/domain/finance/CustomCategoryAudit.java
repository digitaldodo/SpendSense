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
@Table(name = "custom_categories")
public class CustomCategoryAudit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "action", nullable = false, length = 48)
    private String action;

    @Column(name = "before_json", columnDefinition = "text")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "text")
    private String afterJson;

    @Column(name = "reason", length = 240)
    private String reason;

    protected CustomCategoryAudit() {
    }

    public CustomCategoryAudit(UserProfile userProfile, Category category, String action, String beforeJson, String afterJson, String reason) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.category = category;
        this.action = action;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.reason = reason;
    }
}
