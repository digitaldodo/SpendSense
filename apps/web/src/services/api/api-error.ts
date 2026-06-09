export type ApiErrorPayload = {
  code: string;
  message: string;
  details?: Record<string, string[]>;
  traceId?: string;
};

export class ApiError extends Error {
  readonly status: number;
  readonly payload: ApiErrorPayload;

  constructor(status: number, payload: ApiErrorPayload) {
    super(payload.message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}
