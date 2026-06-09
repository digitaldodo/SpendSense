"use client";

import { env } from "@/config/env";
import { getSupabaseBrowserClient } from "@/features/auth/services/auth-client";
import type {
  CsvColumnMapping,
  CsvImportPayload,
  CsvImportSummary,
  CsvPreview,
  ImportFailure,
  ImportJobDetail,
  ImportJob,
  ReconciliationLog,
  SavedImportMapping,
} from "@/features/ingestion/types";
import { ApiError, type ApiErrorPayload } from "@/services/api/api-error";
import type { ApiResponse } from "@/types/api";

async function authenticatedMultipart<T>(path: string, formData: FormData): Promise<T> {
  const supabase = getSupabaseBrowserClient();
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const headers = new Headers();
  if (session?.access_token) {
    headers.set("Authorization", `Bearer ${session.access_token}`);
  }
  const response = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}${path}`, {
    method: "POST",
    headers,
    body: formData,
  });
  const payload = await parseJson<T>(response);
  return payload;
}

async function authenticatedGet<T>(path: string): Promise<T> {
  const supabase = getSupabaseBrowserClient();
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const headers = new Headers();
  if (session?.access_token) {
    headers.set("Authorization", `Bearer ${session.access_token}`);
  }
  const response = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}${path}`, { headers });
  return parseJson<T>(response);
}

async function authenticatedJson<T>(path: string, options: RequestInit = {}): Promise<T> {
  const supabase = getSupabaseBrowserClient();
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (session?.access_token) {
    headers.set("Authorization", `Bearer ${session.access_token}`);
  }
  const response = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}${path}`, { ...options, headers });
  return parseJson<T>(response);
}

async function parseJson<T>(response: Response): Promise<T> {
  const payload = await response.json();
  if (!response.ok) {
    throw new ApiError(response.status, normalizeErrorPayload(payload, response.status));
  }
  return payload as T;
}

function normalizeErrorPayload(payload: unknown, status: number): ApiErrorPayload {
  if (payload && typeof payload === "object" && "message" in payload && "code" in payload) {
    return payload as ApiErrorPayload;
  }
  return {
    code: `HTTP_${status}`,
    message: "The import could not be completed. Please try again.",
  };
}

function toFormData(payload: CsvImportPayload | { file: File; mapping?: CsvColumnMapping }) {
  const formData = new FormData();
  formData.set("file", payload.file);
  if ("mapping" in payload && payload.mapping) {
    formData.set("mapping", JSON.stringify(payload.mapping));
  }
  if ("accountId" in payload && payload.accountId) {
    formData.set("accountId", payload.accountId);
  }
  if ("accountName" in payload && payload.accountName) {
    formData.set("accountName", payload.accountName);
  }
  if ("institutionName" in payload && payload.institutionName) {
    formData.set("institutionName", payload.institutionName);
  }
  if ("idempotencyKey" in payload && payload.idempotencyKey) {
    formData.set("idempotencyKey", payload.idempotencyKey);
  }
  return formData;
}

export async function previewCsvImport(payload: { file: File; mapping?: CsvColumnMapping }) {
  const response = await authenticatedMultipart<ApiResponse<CsvPreview>>(
    "/api/v1/imports/csv/preview",
    toFormData(payload)
  );
  return response.data;
}

export async function confirmCsvImport(payload: CsvImportPayload) {
  const response = await authenticatedMultipart<ApiResponse<CsvImportSummary>>(
    "/api/v1/imports/csv",
    toFormData(payload)
  );
  return response.data;
}

export async function getImportHistory() {
  const response = await authenticatedGet<ApiResponse<ImportJob[]>>("/api/v1/imports");
  return response.data;
}

export async function getImportFailures(jobId: string) {
  const response = await authenticatedGet<ApiResponse<ImportFailure[]>>(`/api/v1/imports/${jobId}/failures`);
  return response.data;
}

export async function getImportDetail(jobId: string) {
  const response = await authenticatedGet<ApiResponse<ImportJobDetail>>(`/api/v1/imports/${jobId}`);
  return response.data;
}

export async function getImportReconciliation(jobId: string) {
  const response = await authenticatedGet<ApiResponse<ReconciliationLog[]>>(
    `/api/v1/imports/${jobId}/reconciliation`
  );
  return response.data;
}

export async function getSavedImportMappings() {
  const response = await authenticatedGet<ApiResponse<SavedImportMapping[]>>("/api/v1/imports/mappings");
  return response.data;
}

export async function renameSavedImportMapping(mappingId: string, name: string) {
  const response = await authenticatedJson<ApiResponse<SavedImportMapping>>(`/api/v1/imports/mappings/${mappingId}`, {
    method: "PATCH",
    body: JSON.stringify({ name }),
  });
  return response.data;
}

export async function deleteSavedImportMapping(mappingId: string) {
  const response = await authenticatedJson<ApiResponse<null>>(`/api/v1/imports/mappings/${mappingId}`, {
    method: "DELETE",
  });
  return response.data;
}
