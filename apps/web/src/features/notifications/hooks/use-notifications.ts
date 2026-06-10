"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createScheduledReport,
  deleteScheduledReport,
  dismissNotification,
  getNotificationDashboard,
  getNotificationPreferences,
  getNotificationSummary,
  getNotifications,
  getReportDeliveryLogs,
  getScheduledReports,
  markAllNotificationsRead,
  markNotificationRead,
  updateNotificationPreferences,
  updateScheduledReport,
} from "@/features/notifications/services/notifications-api";
import type {
  Notification,
  NotificationPreferencePayload,
  NotificationSummary,
  ScheduledReportPayload,
} from "@/features/finance/types";

export const notificationSummaryQueryKey = ["notifications", "summary"] as const;
export const notificationsQueryKey = ["notifications", "list"] as const;
export const notificationDashboardQueryKey = ["notifications", "dashboard"] as const;
export const notificationPreferencesQueryKey = ["notifications", "preferences"] as const;
export const scheduledReportsQueryKey = ["notifications", "scheduled-reports"] as const;
export const reportDeliveryLogsQueryKey = ["notifications", "delivery-logs"] as const;

export function useNotificationSummary() {
  return useQuery({
    queryKey: notificationSummaryQueryKey,
    queryFn: getNotificationSummary,
  });
}

export function useNotifications(filters: { unreadOnly?: boolean } = {}) {
  return useQuery({
    queryKey: [...notificationsQueryKey, filters],
    queryFn: () => getNotifications(filters),
  });
}

export function useNotificationDashboard() {
  return useQuery({
    queryKey: notificationDashboardQueryKey,
    queryFn: getNotificationDashboard,
  });
}

export function useNotificationPreferences() {
  return useQuery({
    queryKey: notificationPreferencesQueryKey,
    queryFn: getNotificationPreferences,
  });
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: NotificationPreferencePayload) => updateNotificationPreferences(payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markNotificationRead,
    async onMutate(notificationId) {
      await queryClient.cancelQueries({ queryKey: ["notifications"] });
      const previousList = queryClient.getQueriesData<Notification[]>({ queryKey: notificationsQueryKey });
      const previousSummary = queryClient.getQueryData<NotificationSummary>(notificationSummaryQueryKey);
      const mark = (item: Notification) => (item.id === notificationId ? { ...item, read: true, readAt: new Date().toISOString() } : item);
      previousList.forEach(([key, value]) => {
        if (value) {
          queryClient.setQueryData(key, value.map(mark));
        }
      });
      if (previousSummary) {
        queryClient.setQueryData(notificationSummaryQueryKey, {
          ...previousSummary,
          unreadCount: Math.max(0, previousSummary.unreadCount - 1),
          latest: previousSummary.latest.map(mark),
          timeline: previousSummary.timeline.map(mark),
        });
      }
      return { previousList, previousSummary };
    },
    onError(_error, _id, context) {
      context?.previousList.forEach(([key, value]) => queryClient.setQueryData(key, value));
      queryClient.setQueryData(notificationSummaryQueryKey, context?.previousSummary);
    },
    onSettled() {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markAllNotificationsRead,
    async onMutate() {
      await queryClient.cancelQueries({ queryKey: ["notifications"] });
      const previousList = queryClient.getQueriesData<Notification[]>({ queryKey: notificationsQueryKey });
      const previousSummary = queryClient.getQueryData<NotificationSummary>(notificationSummaryQueryKey);
      const now = new Date().toISOString();
      previousList.forEach(([key, value]) => {
        if (value) {
          queryClient.setQueryData(
            key,
            value.map((item) => ({ ...item, read: true, readAt: item.readAt ?? now }))
          );
        }
      });
      if (previousSummary) {
        queryClient.setQueryData(notificationSummaryQueryKey, {
          ...previousSummary,
          unreadCount: 0,
          latest: previousSummary.latest.map((item) => ({ ...item, read: true, readAt: item.readAt ?? now })),
          timeline: previousSummary.timeline.map((item) => ({ ...item, read: true, readAt: item.readAt ?? now })),
        });
      }
      return { previousList, previousSummary };
    },
    onError(_error, _id, context) {
      context?.previousList.forEach(([key, value]) => queryClient.setQueryData(key, value));
      queryClient.setQueryData(notificationSummaryQueryKey, context?.previousSummary);
    },
    onSettled() {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useDismissNotification() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: dismissNotification,
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useScheduledReports() {
  return useQuery({
    queryKey: scheduledReportsQueryKey,
    queryFn: getScheduledReports,
  });
}

export function useCreateScheduledReport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ScheduledReportPayload) => createScheduledReport(payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useUpdateScheduledReport(scheduleId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ScheduledReportPayload) => updateScheduledReport(scheduleId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useDeleteScheduledReport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteScheduledReport,
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useReportDeliveryLogs() {
  return useQuery({
    queryKey: reportDeliveryLogsQueryKey,
    queryFn: getReportDeliveryLogs,
  });
}
