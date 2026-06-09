export type ApiResponse<T> = {
  success: boolean;
  data: T;
  message?: string;
  traceId?: string;
  timestamp: string;
};
