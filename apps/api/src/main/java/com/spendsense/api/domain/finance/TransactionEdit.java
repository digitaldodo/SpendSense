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
@Table(name = "transaction_edits")
public class TransactionEdit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "edit_type", nullable = false, length = 64)
    private String editType;

    @Column(name = "before_json", columnDefinition = "text")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "text")
    private String afterJson;

    @Column(name = "reason", length = 240)
    private String reason;

    protected TransactionEdit() {
    }

    public TransactionEdit(
            UserProfile userProfile,
            Transaction transaction,
            String editType,
            String beforeJson,
            String afterJson,
            String reason
    ) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.transaction = transaction;
        this.editType = editType;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.reason = reason;
    }
}
