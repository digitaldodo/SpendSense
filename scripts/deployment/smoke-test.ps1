param(
  [Parameter(Mandatory = $true)]
  [string]$WebBaseUrl,
  [Parameter(Mandatory = $true)]
  [string]$ApiBaseUrl
)

$web = $WebBaseUrl.TrimEnd("/")
$api = $ApiBaseUrl.TrimEnd("/")

$webResponse = Invoke-WebRequest -Uri $web -Method Get -TimeoutSec 15
if ($webResponse.StatusCode -lt 200 -or $webResponse.StatusCode -ge 400) {
  throw "Frontend smoke check failed with HTTP $($webResponse.StatusCode)."
}
if ($webResponse.Content -match "Stack trace|Unhandled Runtime Error|DATABASE_PASSWORD|SUPABASE_JWT_SECRET") {
  throw "Frontend smoke check found unsafe error or secret-shaped text."
}

& "$PSScriptRoot\check-health.ps1" -ApiBaseUrl $api

$heartbeat = Invoke-RestMethod -Uri "$api/api/v1/health/heartbeat" -Method Get -TimeoutSec 10
if ($heartbeat.data.service -ne "spendsense-api") {
  throw "Heartbeat check returned unexpected service: $($heartbeat | ConvertTo-Json -Compress)"
}

$metrics = Invoke-RestMethod -Uri "$api/api/v1/health/metrics" -Method Get -TimeoutSec 10
if ([string]::IsNullOrWhiteSpace($metrics.data.status)) {
  throw "Queue/job monitoring smoke check failed: $($metrics | ConvertTo-Json -Compress)"
}

Write-Host "SpendSense smoke tests passed for $web and $api with health, release, queue, and safe-error checks."
