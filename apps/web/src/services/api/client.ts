import { env } from "@/config/env";
import { ApiError, type ApiErrorPayload } from "@/services/api/api-error";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get("content-type") ?? "";
  const hasJson = contentType.includes("application/json");
  const payload = hasJson ? await response.json() : null;

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
    message: "The request could not be completed. Please try again.",
  };
}

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);

  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  return parseResponse<T>(response);
}
