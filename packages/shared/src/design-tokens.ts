export const designTokens = {
  radius: {
    sm: "0.25rem",
    md: "0.5rem",
    lg: "0.625rem",
    xl: "0.75rem",
  },
  zIndex: {
    base: 0,
    nav: 20,
    overlay: 40,
    modal: 50,
    toast: 60,
  },
  timing: {
    fast: "120ms",
    base: "180ms",
    slow: "260ms",
  },
} as const;

export type DesignTokens = typeof designTokens;
