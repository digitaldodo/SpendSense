import { existsSync, readdirSync, readFileSync } from "node:fs";
import path from "node:path";
import { gzipSync } from "node:zlib";

const root = process.cwd();
const budgets = JSON.parse(readFileSync(path.join(root, "config/performance-budgets.json"), "utf8"));
const buildManifestPath = path.join(root, ".next/build-manifest.json");

if (!existsSync(buildManifestPath)) {
  fail("Next build output is missing. Run npm run build before npm run budget:bundle.");
}

const buildManifest = JSON.parse(readFileSync(buildManifestPath, "utf8"));
const staticDir = path.join(root, ".next/static");

const assetSizeCache = new Map();
function assetSizeKb(asset) {
  const normalized = asset.replace(/^\/+/, "");
  if (assetSizeCache.has(normalized)) {
    return assetSizeCache.get(normalized);
  }
  const fullPath = path.join(root, ".next", normalized.replace(/^_next\//, ""));
  const size = existsSync(fullPath) ? gzipSizeKb(fullPath) : 0;
  assetSizeCache.set(normalized, size);
  return size;
}

function totalKb(assets, extension) {
  return assets
    .filter((asset) => asset.endsWith(extension))
    .reduce((sum, asset) => sum + assetSizeKb(asset), 0);
}

const initialAssets = [
  ...(buildManifest.polyfillFiles ?? []),
  ...(buildManifest.rootMainFiles ?? []),
  ...((buildManifest.pages ?? {})["/_app"] ?? []),
];
const initialJsKb = totalKb(initialAssets, ".js");
const jsAssets = listFiles(path.join(staticDir, "chunks"), ".js").map((filePath) => ({
  asset: path.relative(staticDir, filePath),
  kb: gzipSizeKb(filePath),
}));
const largestJsAsset = jsAssets.reduce(
  (largest, entry) => (entry.kb > largest.kb ? entry : largest),
  { asset: "none", kb: 0 }
);
const totalCssKb = listFiles(staticDir, ".css").reduce(
  (sum, filePath) => sum + gzipSizeKb(filePath),
  0
);

const failures = [];
if (initialJsKb > budgets.bundle.maxInitialJsKb) {
  failures.push(
    `Initial JS ${format(initialJsKb)}KB exceeds ${budgets.bundle.maxInitialJsKb}KB.`
  );
}
if (largestJsAsset.kb > budgets.bundle.maxLargestJsAssetKb) {
  failures.push(
    `${largestJsAsset.asset} JS ${format(largestJsAsset.kb)}KB exceeds ${budgets.bundle.maxLargestJsAssetKb}KB.`
  );
}
if (totalCssKb > budgets.bundle.maxCssKb) {
  failures.push(`CSS ${format(totalCssKb)}KB exceeds ${budgets.bundle.maxCssKb}KB.`);
}

const summary = {
  initialJsKb: Number(format(initialJsKb)),
  largestJsAssetKb: Number(format(largestJsAsset.kb)),
  largestJsAsset: largestJsAsset.asset,
  totalCssKb: Number(format(totalCssKb)),
};

console.log(JSON.stringify(summary, null, 2));

if (failures.length > 0) {
  fail(failures.join("\n"));
}

function listFiles(directory, extension) {
  if (!existsSync(directory)) {
    return [];
  }
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      return listFiles(fullPath, extension);
    }
    return entry.name.endsWith(extension) ? [fullPath] : [];
  });
}

function gzipSizeKb(filePath) {
  return gzipSync(readFileSync(filePath)).length / 1024;
}

function format(value) {
  return value.toFixed(1);
}

function fail(message) {
  console.error(message);
  process.exit(1);
}
