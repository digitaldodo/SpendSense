import { spawn } from "node:child_process";
import { createRequire } from "node:module";

const webRoot = process.cwd();
const require = createRequire(import.meta.url);
const nextBin = require.resolve("next/dist/bin/next");
const port = process.env.PLAYWRIGHT_PORT ?? "3100";
const host = "127.0.0.1";
const baseUrl = `http://${host}:${port}`;

const server = spawn(process.execPath, [nextBin, "dev", "-H", host, "-p", port], {
  cwd: webRoot,
  env: {
    ...withWindowsSystemPath(process.env),
    NEXT_PUBLIC_API_BASE_URL: baseUrl,
    NEXT_PUBLIC_APP_ENV: "development",
    NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS: "1",
    NEXT_PUBLIC_SITE_URL: baseUrl,
  },
  stdio: "inherit",
  windowsHide: true,
});

let shuttingDown = false;

function shutdown(signal) {
  if (shuttingDown) {
    return;
  }
  shuttingDown = true;
  terminateProcessTree(server, signal);
  setTimeout(() => {
    process.exit(0);
  }, 5_000).unref();
}

server.on("exit", (code, signal) => {
  if (signal && !shuttingDown) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 0);
});

process.on("SIGINT", () => shutdown("SIGINT"));
process.on("SIGTERM", () => shutdown("SIGTERM"));

function terminateProcessTree(child, signal) {
  if (!child.pid) {
    return;
  }

  if (process.platform === "win32") {
    const killer = spawn("C:\\Windows\\System32\\taskkill.exe", ["/pid", String(child.pid), "/T", "/F"], {
      env: withWindowsSystemPath(process.env),
      stdio: "ignore",
      windowsHide: true,
    });
    killer.on("error", () => {
      child.kill(signal);
      process.exit(0);
    });
    killer.on("exit", () => process.exit(0));
    return;
  }

  child.kill(signal);
}

function withWindowsSystemPath(env) {
  if (process.platform !== "win32") {
    return env;
  }

  const pathKey = Object.keys(env).find((key) => key.toLowerCase() === "path") ?? "Path";
  const system32 = "C:\\Windows\\System32";
  const currentPath = env[pathKey] ?? "";
  const pathSegments = currentPath.split(";").map((value) => value.trim().toLowerCase());
  const nextPath = pathSegments.includes(system32.toLowerCase())
    ? currentPath
    : `${system32};${currentPath}`;
  return {
    ...env,
    Path: nextPath,
    PATH: nextPath,
    [pathKey]: nextPath,
  };
}
