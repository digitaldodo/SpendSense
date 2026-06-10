"use client";

import { authenticatedApiClient } from "@/services/api/authenticated-client";
import type { ApiResponse } from "@/types/api";
import type {
  Notification,
  NotificationDashboard,
  NotificationPreferencePayload,
  NotificationPreferences,
  NotificationSummary,
  ReportDeliveryLog,
  ScheduledReport,
  ScheduledReportPayload,
} from "@/features/finance/types";

export async function getNotificationSummary() {
  const response = await authenticatedApiClient<ApiResponse<NotificationSummary>>("/api/v1/notifications/summary");
  return response.data;
}

export async function getNotifications(filters: { unreadOnly?: boolean } = {}) {
  const query = filters.unreadOnly ? "?unreadOnly=true" : "";
  const response = await authenticatedApiClient<ApiResponse<Notification[]>>(`/api/v1/notifications${query}`);
  return response.data;
}

export async function getNotificationDashboard() {
  const response = await authenticatedApiClient<ApiResponse<NotificationDashboard>>("/api/v1/notifications/dashboard");
  return response.data;
}

export async function getNotificationPreferences() {
  const response = await authenticatedApiClient<ApiResponse<NotificationPreferences>>("/api/v1/notifications/preferences");
  return response.data;
}

export async function updateNotificationPreferences(payload: NotificationPreferencePayload) {
  const response = await authenticatedApiClient<ApiResponse<NotificationPreferences>>("/api/v1/notifications/preferences", {
    method: "PATCH",
    body: payload,
  });
  return response.data;
}

export async function markNotificationRead(notificationId: string) {
  const response = await authenticatedApiClient<ApiResponse<Notification>>(`/api/v1/notifications/${notificationId}/read`, {
    method: "PATCH",
  });
  return response.data;
}

export async function markAllNotificationsRead() {
  const response = await authenticatedApiClient<ApiResponse<number>>("/api/v1/notifications/read-all", {
    method: "PATCH",
  });
  return response.data;
}

export async function dismissNotification(notificationId: string) {
  const response = await authenticatedApiClient<ApiResponse<null>>(`/api/v1/notifications/${notificationId}`, {
    method: "DELETE",
  });
  return response.data;
}

export async function getScheduledReports() {
  const response = await authenticatedApiClient<ApiResponse<ScheduledReport[]>>("/api/v1/notifications/scheduled-reports");
  return response.data;
}

export async function createScheduledReport(payload: ScheduledReportPayload) {
  const response = await authenticatedApiClient<ApiResponse<ScheduledReport>>("/api/v1/notifications/scheduled-reports", {
    method: "POST",
    body: payload,
  });
  return response.data;
}

export async function updateScheduledReport(scheduleId: string, payload: ScheduledReportPayload) {
  const response = await authenticatedApiClient<ApiResponse<ScheduledReport>>(
    `/api/v1/notifications/scheduled-reports/${scheduleId}`,
    {
      method: "PATCH",
      body: payload,
    }
  );
  return response.data;
}

export async function deleteScheduledReport(scheduleId: string) {
  const response = await authenticatedApiClient<ApiResponse<null>>(`/api/v1/notifications/scheduled-reports/${scheduleId}`, {
    method: "DELETE",
  });
  return response.data;
}

export async function getReportDeliveryLogs() {
  const response = await authenticatedApiClient<ApiResponse<ReportDeliveryLog[]>>(
    "/api/v1/notifications/scheduled-reports/delivery-logs"
  );
  return response.data;
}
