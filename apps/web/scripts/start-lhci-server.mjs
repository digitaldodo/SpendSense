import { spawn } from "node:child_process";
import path from "node:path";

const webRoot = process.cwd();
const nextBin = path.join(webRoot, "node_modules", "next", "dist", "bin", "next");

const server = spawn(process.execPath, [nextBin, "start", "-H", "127.0.0.1", "-p", "3000"], {
  cwd: webRoot,
  env: {
    ...process.env,
    NEXT_PUBLIC_APP_ENV: "development",
    NEXT_PUBLIC_API_BASE_URL: "http://127.0.0.1:3000",
    NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS: "1",
    NEXT_PUBLIC_SITE_URL: "http://127.0.0.1:3000",
    SPENDSENSE_ENABLE_CI_MOCK_API: "1",
  },
  stdio: "inherit",
});

server.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 0);
});

process.on("SIGINT", () => server.kill("SIGINT"));
process.on("SIGTERM", () => server.kill("SIGTERM"));
