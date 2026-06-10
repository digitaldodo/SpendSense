param(
  [Parameter(Mandatory = $true)]
  [string]$ApiBaseUrl,
  [ValidateSet("UP", "DEGRADED", "MAINTENANCE", "DOWN")]
  [string]$ExpectedStatus = "UP",
  [string]$ExpectedEnvironment = "staging",
  [string]$ExpectedCommit = "",
  [switch]$RequireQueueMonitoring
)

$base = $ApiBaseUrl.TrimEnd("/")
$deployment = Invoke-RestMethod -Uri "$base/api/v1/health/deployment" -Method Get -TimeoutSec 10
$version = Invoke-RestMethod -Uri "$base/api/v1/health/version" -Method Get -TimeoutSec 10

if ($deployment.data.status -ne $ExpectedStatus) {
  throw "Incident drill expected deployment status '$ExpectedStatus' but got '$($deployment.data.status)': $($deployment | ConvertTo-Json -Compress)"
}

if ($version.data.environment -ne $ExpectedEnvironment) {
  throw "Incident drill expected environment '$ExpectedEnvironment' but got '$($version.data.environment)'."
}

if (-not [string]::IsNullOrWhiteSpace($ExpectedCommit) -and $version.data.commit -ne $ExpectedCommit) {
  throw "Incident drill expected commit '$ExpectedCommit' but got '$($version.data.commit)'."
}

if ($RequireQueueMonitoring) {
  $metrics = Invoke-RestMethod -Uri "$base/api/v1/health/metrics" -Method Get -TimeoutSec 10
  if ([string]::IsNullOrWhiteSpace($metrics.data.status)) {
    throw "Queue monitoring did not return a status: $($metrics | ConvertTo-Json -Compress)"
  }
}

Write-Host "Incident drill check passed for $ExpectedEnvironment at $($version.data.commit) with status $ExpectedStatus."
