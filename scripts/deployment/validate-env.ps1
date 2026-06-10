param(
  [ValidateSet("staging", "production")]
  [string]$Environment = "staging",
  [string]$EnvFile = ".env.staging.example"
)

if ($Environment -eq "production" -and $EnvFile -eq ".env.staging.example") {
  $EnvFile = ".env.production.example"
}

$required = @(
  "NEXT_PUBLIC_APP_ENV",
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
  -not $values.ContainsKey($_) -or [string]::IsNullOrWhiteSpace($values[$_]) -or $values[$_] -match "localhost|127\.0\.0\.1|local-password|your-|managed-git-sha"
}

if ($missing.Count -gt 0) {
  Write-Error "$Environment env is missing managed values: $($missing -join ', ')"
  exit 1
}

$urls = @("NEXT_PUBLIC_SITE_URL", "NEXT_PUBLIC_API_BASE_URL", "NEXT_PUBLIC_SUPABASE_URL", "PUBLIC_BASE_URL", "WEB_ORIGIN", "SUPABASE_JWT_ISSUER", "SUPABASE_JWKS_URI")
$insecure = $urls | Where-Object { $values[$_] -notmatch "^https://" }
if ($insecure.Count -gt 0) {
  Write-Error "$Environment env must use HTTPS for: $($insecure -join ', ')"
  exit 1
}

if ($values["NEXT_PUBLIC_APP_ENV"] -ne $Environment -or $values["SPENDSENSE_ENVIRONMENT"] -ne $Environment) {
  Write-Error "Environment labels must both equal '$Environment'."
  exit 1
}

Write-Host "$Environment env shape looks ready for managed deployment."
