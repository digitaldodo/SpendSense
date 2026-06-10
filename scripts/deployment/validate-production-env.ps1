param(
  [string]$EnvFile = ".env.production.example"
)

$required = @(
  "NEXT_PUBLIC_SITE_URL",
  "NEXT_PUBLIC_API_BASE_URL",
  "NEXT_PUBLIC_SUPABASE_URL",
  "NEXT_PUBLIC_SUPABASE_ANON_KEY",
  "NEXT_PUBLIC_RELEASE_COMMIT",
  "DATABASE_URL",
  "DATABASE_USERNAME",
  "DATABASE_PASSWORD",
  "PUBLIC_BASE_URL",
  "WEB_ORIGIN",
  "SUPABASE_JWT_ISSUER",
  "SUPABASE_JWKS_URI",
  "SPENDSENSE_ENVIRONMENT",
  "SPENDSENSE_RELEASE_COMMIT"
)

if (-not (Test-Path -LiteralPath $EnvFile)) {
  throw "Environment file not found: $EnvFile"
}

$values = @{}
Get-Content -LiteralPath $EnvFile | ForEach-Object {
  if ($_ -match "^\s*#" -or $_ -notmatch "=") { return }
  $key, $value = $_ -split "=", 2
  $values[$key.Trim()] = $value.Trim()
}

$missing = $required | Where-Object {
  -not $values.ContainsKey($_) -or [string]::IsNullOrWhiteSpace($values[$_]) -or $values[$_] -match "your-|localhost|127\.0\.0\.1|local-password|managed-git-sha"
}

if ($missing.Count -gt 0) {
  Write-Error "Production env is missing managed values: $($missing -join ', ')"
  exit 1
}

Write-Host "Production env shape looks ready."
