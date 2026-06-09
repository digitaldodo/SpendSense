import type { Account, IngestionSource, TransactionDirection } from "@/features/finance/types";

export type ImportJobStatus =
  | "STARTED"
  | "PREVIEWED"
  | "PROCESSING"
  | "COMPLETED"
  | "COMPLETED_WITH_ERRORS"
  | "FAILED";

export type ImportFailureSeverity = "WARNING" | "ERROR";

export type CsvColumnMapping = {
  date?: string | null;
  amount?: string | null;
  debitAmount?: string | null;
  creditAmount?: string | null;
  direction?: string | null;
  merchant?: string | null;
  description?: string | null;
  reference?: string | null;
  balance?: string | null;
  currency?: string | null;
};

export type CsvPreviewRow = {
  rowNumber: number;
  raw: Record<string, string>;
  occurredAt?: string | null;
  amount?: number | null;
  direction?: TransactionDirection | null;
  merchantName?: string | null;
  description?: string | null;
  reference?: string | null;
  duplicate: boolean;
  warning?: string | null;
};

export type ImportFailure = {
  id?: string | null;
  rowNumber: number;
  errorCode: string;
  message: string;
  severity: ImportFailureSeverity;
  rawRowJson?: string | null;
};

export type CsvPreview = {
  filename: string;
  fileChecksum: string;
  fileSignature: string;
  columns: string[];
  mapping: CsvColumnMapping;
  recordsSeen: number;
  validRows: number;
  failedRows: number;
  duplicateRows: number;
  previewRows: CsvPreviewRow[];
  failures: ImportFailure[];
};

export type ImportJob = {
  id: string;
  source: IngestionSource;
  status: ImportJobStatus;
  originalFilename: string;
  fileChecksum: string;
  idempotencyKey?: string | null;
  recordsSeen: number;
  recordsImported: number;
  recordsDuplicate: number;
  recordsFailed: number;
  startedAt: string;
  completedAt?: string | null;
  account?: Account | null;
};

export type CsvImportSummary = {
  job: ImportJob;
  recordsSeen: number;
  recordsImported: number;
  recordsDuplicate: number;
  recordsFailed: number;
  failures: ImportFailure[];
};

export type CsvImportPayload = {
  file: File;
  mapping: CsvColumnMapping;
  accountId?: string;
  accountName?: string;
  institutionName?: string;
  idempotencyKey?: string;
};
