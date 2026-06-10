param(
  [Parameter(Mandatory = $true)]
  [string]$ApiBaseUrl
)

$base = $ApiBaseUrl.TrimEnd("/")
$live = Invoke-RestMethod -Uri "$base/api/v1/health/live" -Method Get -TimeoutSec 10
$ready = Invoke-RestMethod -Uri "$base/api/v1/health/ready" -Method Get -TimeoutSec 10
$deployment = Invoke-RestMethod -Uri "$base/api/v1/health/deployment" -Method Get -TimeoutSec 10
$dependencies = Invoke-RestMethod -Uri "$base/api/v1/health/dependencies" -Method Get -TimeoutSec 10
$version = Invoke-RestMethod -Uri "$base/api/v1/health/version" -Method Get -TimeoutSec 10
$headers = Invoke-WebRequest -Uri "$base/api/v1/health/live" -Method Get -TimeoutSec 10

if ($live.data.status -ne "UP") {
  throw "Liveness check failed: $($live | ConvertTo-Json -Compress)"
}

if ($ready.data.status -ne "UP") {
  throw "Readiness check failed: $($ready | ConvertTo-Json -Compress)"
}

if ($deployment.data.status -ne "UP" -or $dependencies.data.status -ne "UP") {
  throw "Deployment dependency checks failed: deployment=$($deployment.data.status) dependencies=$($dependencies.data.status)"
}

if ([string]::IsNullOrWhiteSpace($version.data.version) -or [string]::IsNullOrWhiteSpace($version.data.environment)) {
  throw "Version metadata check failed: $($version | ConvertTo-Json -Compress)"
}

if ([string]::IsNullOrWhiteSpace($version.data.commit) -or $version.data.commit -eq "local") {
  throw "Release commit metadata is missing or local: $($version | ConvertTo-Json -Compress)"
}

if (-not $headers.Headers.ContainsKey("X-Content-Type-Options")) {
  throw "Security header check failed: X-Content-Type-Options is missing."
}

Write-Host "SpendSense deployment health checks passed for $($version.data.environment) $($version.data.version)."
