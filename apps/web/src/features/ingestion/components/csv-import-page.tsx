"use client";

import { useRef, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  FileText,
  History,
  Loader2,
  RefreshCw,
  ShieldCheck,
  UploadCloud,
  X,
} from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useAccounts } from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import { useConfirmCsvImport, usePreviewCsvImport } from "@/features/ingestion/hooks/use-ingestion";
import type { CsvColumnMapping, CsvImportSummary, CsvPreview } from "@/features/ingestion/types";
import { cn } from "@/lib/utils";

const maxBytes = 5 * 1024 * 1024;

export function CsvImportPage() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const accountsQuery = useAccounts();
  const previewMutation = usePreviewCsvImport();
  const importMutation = useConfirmCsvImport();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<CsvPreview | null>(null);
  const [mapping, setMapping] = useState<CsvColumnMapping>({});
  const [localError, setLocalError] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const [accountMode, setAccountMode] = useState<"existing" | "new">("existing");
  const [accountId, setAccountId] = useState("");
  const [accountName, setAccountName] = useState("CSV Imported Account");
  const [institutionName, setInstitutionName] = useState("CSV Import");
  const summary = importMutation.data;
  const accounts = accountsQuery.data ?? [];
  const canUseExistingAccount = accounts.length > 0;

  function validateFile(nextFile: File) {
    if (!nextFile.name.toLowerCase().endsWith(".csv")) {
      return "Choose a .csv file.";
    }
    if (nextFile.size > maxBytes) {
      return "CSV file must be 5 MB or smaller.";
    }
    if (nextFile.size === 0) {
      return "This CSV file is empty.";
    }
    return null;
  }

  async function acceptFile(nextFile: File) {
    const error = validateFile(nextFile);
    setLocalError(error);
    if (error) {
      return;
    }
    setFile(nextFile);
    setPreview(null);
    importMutation.reset();
    const nextPreview = await previewMutation.mutateAsync({ file: nextFile }).catch(() => null);
    if (nextPreview) {
      setPreview(nextPreview);
      setMapping(nextPreview.mapping);
      if (!accountId && accounts[0]) {
        setAccountId(accounts[0].id);
      }
      if (!canUseExistingAccount) {
        setAccountMode("new");
      }
    }
  }

  async function refreshPreview() {
    if (!file) {
      return;
    }
    const nextPreview = await previewMutation.mutateAsync({ file, mapping }).catch(() => null);
    if (nextPreview) {
      setPreview(nextPreview);
      setMapping(nextPreview.mapping);
    }
  }

  async function confirmImport() {
    if (!file || !preview) {
      return;
    }
    await importMutation.mutateAsync({
      file,
      mapping,
      accountId: accountMode === "existing" ? accountId : undefined,
      accountName: accountMode === "new" ? accountName : undefined,
      institutionName: accountMode === "new" ? institutionName : undefined,
      idempotencyKey: `csv:${preview.fileChecksum}`,
    });
  }

  const errorMessage = localError ?? previewMutation.error?.message ?? importMutation.error?.message ?? null;

  return (
    <main className="grid gap-5">
      <section className="flex flex-col gap-4 rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6 lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-2">
          <div className="inline-flex w-fit items-center gap-2 rounded-lg border border-border bg-muted/55 px-3 py-1.5 text-sm font-medium text-muted-foreground">
            <ShieldCheck className="size-4 text-primary" aria-hidden />
            Secure CSV ingestion
          </div>
          <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">Import transactions with confidence.</h2>
          <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
            Preview rows, confirm column mapping, and review duplicates before anything reaches your ledger.
          </p>
        </div>
        <Button variant="outline" render={<Link href="/imports/history" />}>
          <History className="size-4" aria-hidden />
          History
        </Button>
      </section>

      <section className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="grid gap-4">
          <UploadZone
            dragging={dragging}
            file={file}
            loading={previewMutation.isPending}
            onBrowse={() => fileInputRef.current?.click()}
            onClear={() => {
              setFile(null);
              setPreview(null);
              importMutation.reset();
            }}
            onDropFile={acceptFile}
            setDragging={setDragging}
          />
          <input
            ref={fileInputRef}
            className="hidden"
            type="file"
            accept=".csv,text/csv"
            onChange={(event) => {
              const nextFile = event.target.files?.[0];
              if (nextFile) {
                void acceptFile(nextFile);
              }
            }}
          />

          {errorMessage ? (
            <div className="flex items-start gap-3 rounded-lg border border-destructive/35 bg-destructive/10 p-3 text-sm text-destructive">
              <AlertTriangle className="mt-0.5 size-4" aria-hidden />
              <span>{errorMessage}</span>
            </div>
          ) : null}

          <AccountPanel
            accountId={accountId}
            accountMode={accountMode}
            accountName={accountName}
            accounts={accounts}
            canUseExistingAccount={canUseExistingAccount}
            institutionName={institutionName}
            setAccountId={setAccountId}
            setAccountMode={setAccountMode}
            setAccountName={setAccountName}
            setInstitutionName={setInstitutionName}
          />
        </div>

        <div className="grid gap-4">
          {previewMutation.isPending ? <PreviewLoading /> : null}
          {preview ? (
            <>
              <ImportConfidence preview={preview} />
              <ColumnMappingPanel columns={preview.columns} mapping={mapping} setMapping={setMapping} onRefresh={refreshPreview} />
              <PreviewTable preview={preview} />
              <FailureList preview={preview} />
              <ConfirmPanel
                disabled={
                  importMutation.isPending ||
                  !file ||
                  !preview ||
                  (accountMode === "existing" && !accountId) ||
                  (accountMode === "new" && (!accountName.trim() || !institutionName.trim()))
                }
                loading={importMutation.isPending}
                preview={preview}
                summary={summary}
                onConfirm={confirmImport}
              />
            </>
          ) : (
            <EmptyPreview />
          )}
        </div>
      </section>
    </main>
  );
}

function UploadZone({
  dragging,
  file,
  loading,
  onBrowse,
  onClear,
  onDropFile,
  setDragging,
}: {
  dragging: boolean;
  file: File | null;
  loading: boolean;
  onBrowse: () => void;
  onClear: () => void;
  onDropFile: (file: File) => void;
  setDragging: (dragging: boolean) => void;
}) {
  return (
    <div
      className={cn(
        "grid min-h-64 place-items-center rounded-lg border border-dashed bg-card p-5 text-center shadow-raised transition-colors",
        dragging ? "border-primary bg-primary/5" : "border-border"
      )}
      onDragOver={(event) => {
        event.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(event) => {
        event.preventDefault();
        setDragging(false);
        const nextFile = event.dataTransfer.files?.[0];
        if (nextFile) {
          void onDropFile(nextFile);
        }
      }}
    >
      <div className="grid max-w-sm gap-4 justify-items-center">
        <div className="grid size-12 place-items-center rounded-lg bg-primary/10 text-primary">
          {loading ? <Loader2 className="size-5 animate-spin" aria-hidden /> : <UploadCloud className="size-5" aria-hidden />}
        </div>
        <div className="space-y-1">
          <h3 className="text-lg font-semibold">{file ? file.name : "Drop your CSV here"}</h3>
          <p className="text-sm leading-6 text-muted-foreground">CSV only, up to 5 MB. You will preview everything before import.</p>
        </div>
        <div className="flex flex-wrap justify-center gap-2">
          <Button onClick={onBrowse}>
            <FileText className="size-4" aria-hidden />
            Choose file
          </Button>
          {file ? (
            <Button variant="ghost" onClick={onClear}>
              <X className="size-4" aria-hidden />
              Clear
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function AccountPanel({
  accountId,
  accountMode,
  accountName,
  accounts,
  canUseExistingAccount,
  institutionName,
  setAccountId,
  setAccountMode,
  setAccountName,
  setInstitutionName,
}: {
  accountId: string;
  accountMode: "existing" | "new";
  accountName: string;
  accounts: { id: string; displayName: string }[];
  canUseExistingAccount: boolean;
  institutionName: string;
  setAccountId: (value: string) => void;
  setAccountMode: (value: "existing" | "new") => void;
  setAccountName: (value: string) => void;
  setInstitutionName: (value: string) => void;
}) {
  return (
    <div className="grid gap-3 rounded-lg border border-border bg-card p-4 shadow-raised">
      <div>
        <h3 className="font-semibold">Import destination</h3>
        <p className="text-sm text-muted-foreground">Choose where these transactions should appear.</p>
      </div>
      <div className="grid grid-cols-2 gap-2">
        <Button
          variant={accountMode === "existing" ? "default" : "outline"}
          disabled={!canUseExistingAccount}
          onClick={() => setAccountMode("existing")}
        >
          Existing
        </Button>
        <Button variant={accountMode === "new" ? "default" : "outline"} onClick={() => setAccountMode("new")}>
          New account
        </Button>
      </div>
      {accountMode === "existing" ? (
        <select
          className="h-10 rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-ring focus:ring-3 focus:ring-ring/50"
          value={accountId}
          onChange={(event) => setAccountId(event.target.value)}
        >
          <option value="">Select account</option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.displayName}
            </option>
          ))}
        </select>
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          <Input value={accountName} onChange={(event) => setAccountName(event.target.value)} placeholder="Account name" />
          <Input
            value={institutionName}
            onChange={(event) => setInstitutionName(event.target.value)}
            placeholder="Institution"
          />
        </div>
      )}
    </div>
  );
}

function ImportConfidence({ preview }: { preview: CsvPreview }) {
  const items = [
    { label: "Rows found", value: preview.recordsSeen },
    { label: "Ready", value: preview.validRows },
    { label: "Duplicates", value: preview.duplicateRows },
    { label: "Errors", value: preview.failedRows },
  ];
  return (
    <div className="grid gap-2 rounded-lg border border-border bg-card p-4 shadow-raised sm:grid-cols-4">
      {items.map((item) => (
        <div key={item.label} className="rounded-lg bg-muted/45 px-3 py-2">
          <p className="text-xs font-medium text-muted-foreground">{item.label}</p>
          <p className="text-xl font-semibold">{item.value}</p>
        </div>
      ))}
    </div>
  );
}

function ColumnMappingPanel({
  columns,
  mapping,
  onRefresh,
  setMapping,
}: {
  columns: string[];
  mapping: CsvColumnMapping;
  onRefresh: () => void;
  setMapping: (mapping: CsvColumnMapping) => void;
}) {
  const fields = [
    ["date", "Date"],
    ["amount", "Amount"],
    ["debitAmount", "Debit"],
    ["creditAmount", "Credit"],
    ["direction", "Type"],
    ["merchant", "Merchant"],
    ["description", "Description"],
    ["reference", "Reference"],
  ] as const;
  return (
    <div className="grid gap-3 rounded-lg border border-border bg-card p-4 shadow-raised">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h3 className="font-semibold">Column mapping</h3>
          <p className="text-sm text-muted-foreground">Detected columns can be adjusted before import.</p>
        </div>
        <Button variant="outline" size="sm" onClick={onRefresh}>
          <RefreshCw className="size-4" aria-hidden />
          Recheck
        </Button>
      </div>
      <div className="grid gap-2 sm:grid-cols-2">
        {fields.map(([key, label]) => (
          <label key={key} className="grid gap-1 text-sm">
            <span className="font-medium">{label}</span>
            <select
              className="h-10 min-w-0 rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-ring focus:ring-3 focus:ring-ring/50"
              value={(mapping[key] as string | null | undefined) ?? ""}
              onChange={(event) => setMapping({ ...mapping, [key]: event.target.value || null })}
            >
              <option value="">Not mapped</option>
              {columns.map((column) => (
                <option key={`${key}-${column}`} value={column}>
                  {column}
                </option>
              ))}
            </select>
          </label>
        ))}
      </div>
    </div>
  );
}

function PreviewTable({ preview }: { preview: CsvPreview }) {
  return (
    <div className="overflow-hidden rounded-lg border border-border bg-card shadow-raised">
      <div className="flex items-center justify-between gap-3 border-b border-border px-4 py-3">
        <h3 className="font-semibold">Preview</h3>
        <Badge variant="secondary">{preview.filename}</Badge>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[42rem] text-left text-sm">
          <thead className="bg-muted/55 text-xs uppercase text-muted-foreground">
            <tr>
              <th className="px-4 py-3 font-medium">Row</th>
              <th className="px-4 py-3 font-medium">Date</th>
              <th className="px-4 py-3 font-medium">Merchant</th>
              <th className="px-4 py-3 font-medium">Type</th>
              <th className="px-4 py-3 text-right font-medium">Amount</th>
              <th className="px-4 py-3 font-medium">Status</th>
            </tr>
          </thead>
          <tbody>
            {preview.previewRows.map((row) => (
              <tr key={row.rowNumber} className="border-t border-border/70">
                <td className="px-4 py-3 text-muted-foreground">{row.rowNumber}</td>
                <td className="px-4 py-3">{row.occurredAt ? new Date(row.occurredAt).toLocaleDateString("en-IN") : "-"}</td>
                <td className="max-w-60 truncate px-4 py-3">{row.merchantName || "-"}</td>
                <td className="px-4 py-3">{row.direction || "-"}</td>
                <td className="px-4 py-3 text-right">{typeof row.amount === "number" ? formatMoney(row.amount) : "-"}</td>
                <td className="px-4 py-3">
                  {row.duplicate ? <Badge variant="outline">Duplicate</Badge> : <Badge variant="secondary">Ready</Badge>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function FailureList({ preview }: { preview: CsvPreview }) {
  if (preview.failures.length === 0) {
    return null;
  }
  return (
    <div className="grid gap-2 rounded-lg border border-warning/45 bg-warning/10 p-4">
      <div className="flex items-center gap-2 font-semibold">
        <AlertTriangle className="size-4" aria-hidden />
        Rows needing attention
      </div>
      {preview.failures.slice(0, 5).map((failure) => (
        <div key={`${failure.rowNumber}-${failure.errorCode}`} className="rounded-lg bg-background/70 px-3 py-2 text-sm">
          <span className="font-medium">Row {failure.rowNumber}:</span> {failure.message}
        </div>
      ))}
    </div>
  );
}

function ConfirmPanel({
  disabled,
  loading,
  onConfirm,
  preview,
  summary,
}: {
  disabled: boolean;
  loading: boolean;
  onConfirm: () => void;
  preview: CsvPreview;
  summary?: CsvImportSummary;
}) {
  if (summary) {
    return (
      <div className="grid gap-3 rounded-lg border border-success/40 bg-success/10 p-4">
        <div className="flex items-center gap-2 font-semibold text-success">
          <CheckCircle2 className="size-5" aria-hidden />
          Import complete
        </div>
        <div className="grid gap-2 text-sm sm:grid-cols-4">
          <SummaryCell label="Imported" value={summary.recordsImported} />
          <SummaryCell label="Duplicates" value={summary.recordsDuplicate} />
          <SummaryCell label="Failed" value={summary.recordsFailed} />
          <SummaryCell label="Seen" value={summary.recordsSeen} />
        </div>
        <Button render={<Link href="/dashboard" />}>
          View dashboard
          <ArrowRight className="size-4" aria-hidden />
        </Button>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-border bg-card p-4 shadow-raised sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 className="font-semibold">Ready to import?</h3>
        <p className="text-sm text-muted-foreground">
          {preview.validRows} row(s) will be imported. {preview.duplicateRows} duplicate warning(s) will be skipped.
        </p>
      </div>
      <Button disabled={disabled} onClick={onConfirm}>
        {loading ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <CheckCircle2 className="size-4" aria-hidden />}
        Confirm import
      </Button>
    </div>
  );
}

function SummaryCell({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-background/70 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-lg font-semibold">{value}</p>
    </div>
  );
}

function PreviewLoading() {
  return (
    <div className="grid gap-3 rounded-lg border border-border bg-card p-4 shadow-raised">
      <Skeleton className="h-8 w-44" />
      <Skeleton className="h-24 w-full" />
      <Skeleton className="h-36 w-full" />
    </div>
  );
}

function EmptyPreview() {
  return (
    <div className="grid min-h-80 place-items-center rounded-lg border border-border bg-card p-6 text-center shadow-raised">
      <div className="grid max-w-sm gap-3 justify-items-center">
        <div className="grid size-12 place-items-center rounded-lg bg-muted text-muted-foreground">
          <FileText className="size-5" aria-hidden />
        </div>
        <h3 className="text-lg font-semibold">Preview appears here</h3>
        <p className="text-sm leading-6 text-muted-foreground">
          Upload a CSV to validate rows, detect columns, and check duplicates before confirming.
        </p>
      </div>
    </div>
  );
}
