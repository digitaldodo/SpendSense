"use client";

import { authenticatedApiClient } from "@/services/api/authenticated-client";
import type { ApiResponse } from "@/types/api";
import type {
  OnboardingProgressUpdate,
  OnboardingStatus,
  Profile,
} from "@/features/profile/types";

export async function getCurrentProfile() {
  const response = await authenticatedApiClient<ApiResponse<Profile>>("/api/v1/profile/current");
  return response.data;
}

export async function updateProfile(input: { displayName?: string | null }) {
  const response = await authenticatedApiClient<ApiResponse<Profile>>("/api/v1/profile", {
    method: "PATCH",
    body: input,
  });
  return response.data;
}

export async function getOnboardingStatus() {
  const response = await authenticatedApiClient<ApiResponse<OnboardingStatus>>(
    "/api/v1/onboarding/status"
  );
  return response.data;
}

export async function saveOnboardingProgress(input: OnboardingProgressUpdate) {
  const response = await authenticatedApiClient<ApiResponse<Profile>>("/api/v1/onboarding/progress", {
    method: "PATCH",
    body: input,
  });
  return response.data;
}

export async function completeOnboarding() {
  const response = await authenticatedApiClient<ApiResponse<Profile>>("/api/v1/onboarding/complete", {
    method: "POST",
  });
  return response.data;
}
