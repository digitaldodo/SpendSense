/* eslint-disable @typescript-eslint/no-require-imports */
const budgets = require("./config/performance-budgets.json");

module.exports = {
  ci: {
    collect: {
      numberOfRuns: 1,
      startServerCommand: "node scripts/start-lhci-server.mjs",
      startServerReadyPattern: "Ready",
      startServerReadyTimeout: 120000,
      url: ["http://127.0.0.1:3000/dashboard?lhci=1"],
      settings: {
        chromeFlags: "--headless=new --no-sandbox",
        emulatedFormFactor: "mobile",
        formFactor: "mobile",
        screenEmulation: {
          disabled: false,
          width: 390,
          height: 844,
          deviceScaleFactor: 3,
          mobile: true,
        },
      },
    },
    assert: {
      assertions: {
        "categories:performance": ["error", { minScore: budgets.lighthouse.performance }],
        "categories:accessibility": ["error", { minScore: budgets.lighthouse.accessibility }],
        "categories:best-practices": ["warn", { minScore: budgets.lighthouse.bestPractices }],
        "categories:seo": ["warn", { minScore: budgets.lighthouse.seo }],
        "cumulative-layout-shift": [
          "error",
          { maxNumericValue: budgets.lighthouse.cumulativeLayoutShift },
        ],
        "largest-contentful-paint": [
          "error",
          { maxNumericValue: budgets.lighthouse.largestContentfulPaintMs },
        ],
        "total-blocking-time": [
          "error",
          { maxNumericValue: budgets.lighthouse.totalBlockingTimeMs },
        ],
      },
    },
    upload: {
      target: "temporary-public-storage",
    },
  },
};
