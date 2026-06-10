import { spawn } from "node:child_process";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const nextBin = require.resolve("next/dist/bin/next");
const playwrightBin = require.resolve("@playwright/test/cli");
const host = "127.0.0.1";
const port = process.env.PLAYWRIGHT_PORT ?? "3100";
const baseUrl = `http://${host}:${port}`;
const passthroughArgs = process.argv.slice(2);

const env = {
  ...withWindowsSystemPath(process.env),
  NEXT_PUBLIC_API_BASE_URL: baseUrl,
  NEXT_PUBLIC_APP_ENV: "development",
  NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS: "1",
  NEXT_PUBLIC_SITE_URL: baseUrl,
  PLAYWRIGHT_SKIP_WEB_SERVER: "1",
};

const server = spawn(process.execPath, [nextBin, "dev", "-H", host, "-p", port], {
  cwd: process.cwd(),
  env,
  stdio: "inherit",
  windowsHide: true,
});

let exitCode = 1;

try {
  await waitForReady(baseUrl, 120_000);
  const test = spawn(process.execPath, [playwrightBin, "test", ...passthroughArgs], {
    cwd: process.cwd(),
    env,
    stdio: "inherit",
    windowsHide: true,
  });
  exitCode = await waitForExit(test);
} finally {
  await terminateProcessTree(server);
}

process.exit(exitCode);

function waitForExit(child) {
  return new Promise((resolve) => {
    child.on("exit", (code, signal) => {
      resolve(signal ? 1 : (code ?? 0));
    });
  });
}

async function waitForReady(url, timeoutMs) {
  const startedAt = Date.now();
  let lastError = "";

  while (Date.now() - startedAt < timeoutMs) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(2_000) });
      if (response.status >= 200 && response.status < 500) {
        return;
      }
      lastError = `HTTP ${response.status}`;
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
    }
    await new Promise((resolve) => setTimeout(resolve, 750));
  }

  throw new Error(`Playwright server was not ready at ${url}: ${lastError}`);
}

function terminateProcessTree(child) {
  if (!child.pid) {
    return Promise.resolve();
  }

  if (process.platform === "win32") {
    return new Promise((resolve) => {
      const taskkill = "C:\\Windows\\System32\\taskkill.exe";
      const killer = spawn(taskkill, ["/pid", String(child.pid), "/T", "/F"], {
        env: withWindowsSystemPath(process.env),
        stdio: "ignore",
        windowsHide: true,
      });
      killer.on("error", () => {
        child.kill("SIGTERM");
        resolve();
      });
      killer.on("exit", resolve);
    });
  }

  child.kill("SIGTERM");
  return waitForExit(child);
}

function withWindowsSystemPath(envValues) {
  if (process.platform !== "win32") {
    return envValues;
  }

  const pathKey = Object.keys(envValues).find((key) => key.toLowerCase() === "path") ?? "Path";
  const system32 = "C:\\Windows\\System32";
  const currentPath = envValues[pathKey] ?? "";
  const pathSegments = currentPath.split(";").map((value) => value.trim().toLowerCase());
  const nextPath = pathSegments.includes(system32.toLowerCase())
    ? currentPath
    : `${system32};${currentPath}`;
  return {
    ...envValues,
    Path: nextPath,
    PATH: nextPath,
    [pathKey]: nextPath,
  };
}
