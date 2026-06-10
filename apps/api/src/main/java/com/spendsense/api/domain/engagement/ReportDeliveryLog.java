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
@Table(name = "report_delivery_logs")
public class ReportDeliveryLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_report_id")
    private ScheduledReport scheduledReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "generated_report_id")
    private java.util.UUID generatedReportId;

    @Column(name = "delivery_channel", nullable = false, length = 32)
    private String deliveryChannel;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "error_message", length = 520)
    private String errorMessage;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    protected ReportDeliveryLog() {
    }
}
