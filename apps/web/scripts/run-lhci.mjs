import { spawn } from "node:child_process";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const lhciBin = require.resolve("@lhci/cli/src/cli.js");

const child = spawn(process.execPath, [lhciBin, "autorun"], {
  cwd: process.cwd(),
  env: withWindowsSystemPath(process.env),
  stdio: "inherit",
  windowsHide: true,
});

child.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 0);
});

process.on("SIGINT", () => child.kill("SIGINT"));
process.on("SIGTERM", () => child.kill("SIGTERM"));

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
