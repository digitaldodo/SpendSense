"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  completeOnboarding,
  getCurrentProfile,
  saveOnboardingProgress,
  updateProfile,
} from "@/features/profile/services/profile-api";
import type { OnboardingProgressUpdate } from "@/features/profile/types";

export const profileQueryKey = ["profile", "current"] as const;

export function useProfile(enabled = true) {
  return useQuery({
    queryKey: profileQueryKey,
    queryFn: getCurrentProfile,
    enabled,
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateProfile,
    onSuccess(profile) {
      queryClient.setQueryData(profileQueryKey, profile);
    },
  });
}

export function useSaveOnboardingProgress() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: OnboardingProgressUpdate) => saveOnboardingProgress(input),
    onSuccess(profile) {
      queryClient.setQueryData(profileQueryKey, profile);
    },
  });
}

export function useCompleteOnboarding() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: completeOnboarding,
    onSuccess(profile) {
      queryClient.setQueryData(profileQueryKey, profile);
    },
  });
}
