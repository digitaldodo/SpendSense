package com.spendsense.api.domain.engagement;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "budget_warnings_enabled", nullable = false)
    private boolean budgetWarningsEnabled = true;

    @Column(name = "recurring_reminders_enabled", nullable = false)
    private boolean recurringRemindersEnabled = true;

    @Column(name = "report_ready_enabled", nullable = false)
    private boolean reportReadyEnabled = true;

    @Column(name = "savings_nudges_enabled", nullable = false)
    private boolean savingsNudgesEnabled = true;

    @Column(name = "spending_increase_enabled", nullable = false)
    private boolean spendingIncreaseEnabled = true;

    @Column(name = "weekly_digest_enabled", nullable = false)
    private boolean weeklyDigestEnabled;

    @Column(name = "monthly_report_enabled", nullable = false)
    private boolean monthlyReportEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "email_address", length = 320)
    private String emailAddress;

    @Column(name = "digest_frequency", nullable = false, length = 24)
    private String digestFrequency = "OFF";

    @Column(name = "budget_alert_email_enabled", nullable = false)
    private boolean budgetAlertEmailEnabled;

    @Column(name = "recurring_reminder_email_enabled", nullable = false)
    private boolean recurringReminderEmailEnabled;

    @Column(name = "report_email_enabled", nullable = false)
    private boolean reportEmailEnabled;

    @Column(name = "delivery_failure_alerts_enabled", nullable = false)
    private boolean deliveryFailureAlertsEnabled = true;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone = "Asia/Kolkata";

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    protected NotificationPreference() {
    }
}
