package com.spendsense.api.domain.engagement;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "notification_type", nullable = false, length = 64)
    private String notificationType;

    @Column(name = "severity", nullable = false, length = 24)
    private String severity = "INFO";

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "body", nullable = false, length = 520)
    private String body;

    @Column(name = "action_label", length = 80)
    private String actionLabel;

    @Column(name = "action_url", length = 240)
    private String actionUrl;

    @Column(name = "source_type", length = 64)
    private String sourceType;

    @Column(name = "source_id", length = 120)
    private String sourceId;

    @Column(name = "delivery_channel", nullable = false, length = 32)
    private String deliveryChannel = "IN_APP";

    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private String lifecycleStatus = "ACTIVE";

    @Column(name = "priority", nullable = false)
    private int priority = 3;

    @Column(name = "dedupe_key", length = 180)
    private String dedupeKey;

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Notification() {
    }
}
