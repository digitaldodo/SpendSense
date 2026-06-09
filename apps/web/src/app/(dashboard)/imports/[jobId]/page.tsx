"use client";

import { useParams } from "next/navigation";
import { ImportDetailPage } from "@/features/ingestion/components/import-detail-page";

export default function ImportDetailRoute() {
  const params = useParams<{ jobId: string }>();
  return <ImportDetailPage jobId={params.jobId} />;
}
