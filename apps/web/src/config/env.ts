import { z } from "zod";

const envSchema = z.object({
  NEXT_PUBLIC_APP_ENV: z
    .enum(["local", "development", "staging", "production"])
    .default("local"),
  NEXT_PUBLIC_API_BASE_URL: z.string().url().default("http://localhost:8080"),
  NEXT_PUBLIC_SUPABASE_URL: z.string().url().default("http://localhost:54321"),
  NEXT_PUBLIC_SUPABASE_ANON_KEY: z.string().min(1).default("local-development-anon-key"),
});

export const env = envSchema.parse({
  NEXT_PUBLIC_APP_ENV: process.env.NEXT_PUBLIC_APP_ENV,
  NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
  NEXT_PUBLIC_SUPABASE_URL: process.env.NEXT_PUBLIC_SUPABASE_URL,
  NEXT_PUBLIC_SUPABASE_ANON_KEY: process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY,
});
