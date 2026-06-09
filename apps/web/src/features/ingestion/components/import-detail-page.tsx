"use client";

import { AlertTriangle, ArrowLeft, CheckCircle2, FileUp, Loader2, RotateCcw } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { formatMoney } from "@/features/finance/lib/format";
import { useImportDetail } from "@/features/ingestion/hooks/use-ingestion";

export function ImportDetailPage({ jobId }: { jobId: string }) {
  const detailQuery = useImportDetail(jobId);
  const detail = detailQuery.data;

  if (detailQuery.isLoading) {
    return (
      <main className="grid gap-4">
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-80 w-full" />
      </main>
    );
  }

  if (!detail) {
    return (
      <main className="grid min-h-80 place-items-center rounded-lg border border-border bg-card p-6 text-center shadow-raised">
        <div className="grid gap-3">
          <AlertTriangle className="mx-auto size-8 text-destructive" aria-hidden />
          <h2 className="text-lg font-semibold">Import could not be loaded</h2>
          <Button variant="outline" render={<Link href="/imports/history" />}>
            <ArrowLeft className="size-4" aria-hidden />
            Back to history
          </Button>
        </div>
      </main>
    );
  }

  const { job } = detail;
  const latestReconciliation = detail.reconciliationLogs[0];

  return (
    <main className="grid gap-5">
      <section className="flex flex-col gap-4 rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6 lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-2">
          <Button variant="ghost" size="sm" render={<Link href="/imports/history" />}>
            <ArrowLeft className="size-4" aria-hidden />
            History
          </Button>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">{job.originalFilename}</h2>
            <Badge variant={job.status === "FAILED" ? "outline" : "secondary"}>{job.status.replaceAll("_", " ")}</Badge>
          </div>
          <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
            Imported {new Date(job.startedAt).toLocaleString("en-IN")} into {job.account?.displayName ?? "CSV account"}.
          </p>
        </div>
        <Button variant="outline" render={<Link href="/imports" />}>
          <RotateCcw className="size-4" aria-hidden />
          Retry with CSV
        </Button>
      </section>

      <section className="grid gap-4 md:grid-cols-5">
        <Metric label="Seen" value={job.recordsSeen} />
        <Metric label="Imported" value={job.recordsImported} />
        <Metric label="Duplicates" value={job.recordsDuplicate} />
        <Metric label="Failed" value={job.recordsFailed} />
        <Metric label="Mapping" value={`${Math.round(job.mappingConfidenceScore)}%`} />
      </section>

      <section className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="grid gap-4">
          <div className="rounded-lg border border-border bg-card p-4 shadow-raised">
            <h3 className="font-semibold">Reconciliation</h3>
            {latestReconciliation ? (
              <div className="mt-3 grid gap-2 text-sm">
                <ReconcileLine label="Opening balance" value={formatMoney(latestReconciliation.openingBalance ?? 0)} />
                <ReconcileLine label="Imported delta" value={formatMoney(latestReconciliation.importedBalanceDelta)} />
                <ReconcileLine label="Closing balance" value={formatMoney(latestReconciliation.closingBalance ?? 0)} />
                <ReconcileLine label="Rows reviewed" value={`${latestReconciliation.recordsSeen}`} />
              </div>
            ) : (
              <p className="mt-2 text-sm text-muted-foreground">No reconciliation log was recorded for this import.</p>
            )}
          </div>

          <div className="rounded-lg border border-border bg-card p-4 shadow-raised">
            <h3 className="font-semibold">Mapping used</h3>
            <dl className="mt-3 grid gap-2 text-sm">
              {Object.entries(detail.mapping).map(([key, value]) => (
                <div key={key} className="flex justify-between gap-4 rounded-lg bg-muted/30 px-3 py-2">
                  <dt className="capitalize text-muted-foreground">{key}</dt>
                  <dd className="max-w-44 truncate font-medium">{value || "Not mapped"}</dd>
                </div>
              ))}
            </dl>
          </div>
        </div>

        <div className="rounded-lg border border-border bg-card p-4 shadow-raised">
          <h3 className="font-semibold">Conflicts and row issues</h3>
          <p className="text-sm text-muted-foreground">Duplicates are kept out of the ledger and recorded for review.</p>
          <div className="mt-4 grid gap-2">
            {detail.failures.length === 0 ? (
              <div className="flex items-center gap-2 rounded-lg bg-success/10 p-3 text-sm text-success">
                <CheckCircle2 className="size-4" aria-hidden />
                No conflicts recorded.
              </div>
            ) : (
              detail.failures.map((failure) => (
                <div key={failure.id ?? `${failure.rowNumber}-${failure.errorCode}`} className="rounded-lg border border-border bg-muted/25 p-3 text-sm">
                  <div className="flex flex-wrap items-center gap-2">
                    {failure.severity === "WARNING" ? (
                      <AlertTriangle className="size-4 text-warning" aria-hidden />
                    ) : (
                      <AlertTriangle className="size-4 text-destructive" aria-hidden />
                    )}
                    <span className="font-medium">Row {failure.rowNumber}</span>
                    <Badge variant="outline">{failure.errorCode}</Badge>
                  </div>
                  <p className="mt-1 text-muted-foreground">{failure.message}</p>
                </div>
              ))
            )}
          </div>
        </div>
      </section>

      <div className="flex flex-col gap-2 sm:flex-row">
        <Button render={<Link href="/dashboard" />}>
          <FileUp className="size-4" aria-hidden />
          View dashboard
        </Button>
        {detailQuery.isFetching ? (
          <span className="inline-flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" aria-hidden />
            Refreshing
          </span>
        ) : null}
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-lg border border-border bg-card p-4 shadow-raised">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
    </div>
  );
}

function ReconcileLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border/70 bg-background/60 px-3 py-2">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium tabular-nums">{value}</span>
    </div>
  );
}
