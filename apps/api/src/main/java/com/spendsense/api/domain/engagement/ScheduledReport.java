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
@Table(name = "scheduled_reports")
public class ScheduledReport extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "report_type", nullable = false, length = 48)
    private String reportType;

    @Column(name = "format", nullable = false, length = 16)
    private String format;

    @Column(name = "cadence", nullable = false, length = 24)
    private String cadence;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "Asia/Kolkata";

    @Column(name = "delivery_channel", nullable = false, length = 32)
    private String deliveryChannel = "IN_APP";

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    protected ScheduledReport() {
    }
}
