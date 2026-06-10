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

  if (status === 503) {
    return {
      code: "SERVICE_UNAVAILABLE",
      message: "SpendSense is temporarily unavailable while services recover. Please try again shortly.",
    };
  }

  return {
    code: `HTTP_${status}`,
    message: "The request could not be completed. Please try again.",
  };
}

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  const controller = new AbortController();
  const timeout = globalThis.setTimeout(() => controller.abort(), 15_000);

  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("X-Correlation-Id")) {
    headers.set("X-Correlation-Id", createCorrelationId());
  }

  try {
    const response = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal ?? controller.signal,
    });

    return parseResponse<T>(response);
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiError(408, {
        code: "REQUEST_TIMEOUT",
        message: "The service took too long to respond. Please try again.",
      });
    }
    throw error;
  } finally {
    globalThis.clearTimeout(timeout);
  }
}

function createCorrelationId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
