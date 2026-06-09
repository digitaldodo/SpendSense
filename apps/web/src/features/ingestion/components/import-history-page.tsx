"use client";

import { useState } from "react";
import { AlertTriangle, CheckCircle2, FileClock, FileText, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useImportFailures, useImportHistory } from "@/features/ingestion/hooks/use-ingestion";

export function ImportHistoryPage() {
  const historyQuery = useImportHistory();
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const failuresQuery = useImportFailures(selectedJobId);

  if (historyQuery.isLoading) {
    return (
      <main className="grid gap-4">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-80 w-full" />
      </main>
    );
  }

  const jobs = historyQuery.data ?? [];

  return (
    <main className="grid gap-5">
      <section className="rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6">
        <div className="space-y-2">
          <div className="inline-flex w-fit items-center gap-2 rounded-lg border border-border bg-muted/55 px-3 py-1.5 text-sm font-medium text-muted-foreground">
            <FileClock className="size-4 text-primary" aria-hidden />
            Import traceability
          </div>
          <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">Import history</h2>
          <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
            Review completed CSV imports, duplicate warnings, and failed rows from one place.
          </p>
        </div>
      </section>

      {historyQuery.isError ? (
        <div className="rounded-lg border border-destructive/35 bg-destructive/10 p-4 text-sm text-destructive">
          Import history could not be loaded.
        </div>
      ) : null}

      {jobs.length === 0 ? (
        <div className="grid min-h-72 place-items-center rounded-lg border border-border bg-card p-6 text-center shadow-raised">
          <div className="grid max-w-sm gap-3 justify-items-center">
            <FileText className="size-8 text-muted-foreground" aria-hidden />
            <h3 className="text-lg font-semibold">No imports yet</h3>
            <p className="text-sm text-muted-foreground">Your first CSV import will create an auditable job here.</p>
          </div>
        </div>
      ) : (
        <section className="grid gap-4 lg:grid-cols-[1fr_0.85fr]">
          <div className="overflow-hidden rounded-lg border border-border bg-card shadow-raised">
            {jobs.map((job) => (
              <button
                key={job.id}
                className="grid w-full gap-2 border-b border-border/70 px-4 py-4 text-left transition-colors last:border-b-0 hover:bg-muted/45 sm:grid-cols-[1fr_auto]"
                onClick={() => setSelectedJobId(job.id)}
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="truncate font-semibold">{job.originalFilename}</p>
                    <StatusBadge status={job.status} />
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {new Date(job.startedAt).toLocaleString("en-IN")} · {job.account?.displayName ?? "CSV account"}
                  </p>
                </div>
                <div className="grid grid-cols-4 gap-2 text-center text-sm sm:min-w-72">
                  <Metric label="Seen" value={job.recordsSeen} />
                  <Metric label="Imported" value={job.recordsImported} />
                  <Metric label="Dupes" value={job.recordsDuplicate} />
                  <Metric label="Failed" value={job.recordsFailed} />
                </div>
              </button>
            ))}
          </div>

          <div className="rounded-lg border border-border bg-card p-4 shadow-raised">
            <h3 className="font-semibold">Row issues</h3>
            <p className="text-sm text-muted-foreground">Select an import to inspect warnings and failures.</p>
            <div className="mt-4 grid gap-2">
              {failuresQuery.isLoading ? <Loader2 className="size-5 animate-spin text-muted-foreground" aria-hidden /> : null}
              {(failuresQuery.data ?? []).map((failure) => (
                <div key={failure.id ?? `${failure.rowNumber}-${failure.errorCode}`} className="rounded-lg border border-border bg-muted/25 p-3 text-sm">
                  <div className="flex items-center gap-2">
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
              ))}
              {selectedJobId && !failuresQuery.isLoading && (failuresQuery.data ?? []).length === 0 ? (
                <div className="flex items-center gap-2 rounded-lg bg-success/10 p-3 text-sm text-success">
                  <CheckCircle2 className="size-4" aria-hidden />
                  No row issues recorded for this import.
                </div>
              ) : null}
              {!selectedJobId ? <Button variant="outline" disabled>Select an import</Button> : null}
            </div>
          </div>
        </section>
      )}
    </main>
  );
}

function StatusBadge({ status }: { status: string }) {
  const clean = status.replaceAll("_", " ");
  return <Badge variant={status.includes("ERROR") || status === "FAILED" ? "outline" : "secondary"}>{clean}</Badge>;
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-muted/50 px-2 py-1.5">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="font-semibold">{value}</p>
    </div>
  );
}
