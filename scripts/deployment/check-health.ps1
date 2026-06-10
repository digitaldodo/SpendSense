param(
  [Parameter(Mandatory = $true)]
  [string]$ApiBaseUrl
)

$base = $ApiBaseUrl.TrimEnd("/")
$live = Invoke-RestMethod -Uri "$base/api/v1/health/live" -Method Get -TimeoutSec 10
$ready = Invoke-RestMethod -Uri "$base/api/v1/health/ready" -Method Get -TimeoutSec 10
$version = Invoke-RestMethod -Uri "$base/api/v1/health/version" -Method Get -TimeoutSec 10

if ($live.data.status -ne "UP") {
  throw "Liveness check failed: $($live | ConvertTo-Json -Compress)"
}

if ($ready.data.status -ne "UP") {
  throw "Readiness check failed: $($ready | ConvertTo-Json -Compress)"
}

if ([string]::IsNullOrWhiteSpace($version.data.version) -or [string]::IsNullOrWhiteSpace($version.data.environment)) {
  throw "Version metadata check failed: $($version | ConvertTo-Json -Compress)"
}

Write-Host "SpendSense deployment health checks passed for $($version.data.environment) $($version.data.version)."
