param(
  [Parameter(Mandatory = $true)]
  [string]$WebBaseUrl,
  [Parameter(Mandatory = $true)]
  [string]$ApiBaseUrl,
  [Parameter(Mandatory = $true)]
  [string]$ExpectedCommit,
  [string]$EnvFile = ".env.staging",
  [switch]$RequireQueueMonitoring,
  [switch]$RequireAdminVerification,
  [string]$AdminBearerToken = ""
)

$ErrorActionPreference = "Stop"

function Assert-ManagedUrl([string]$Name, [string]$Value) {
  if ($Value -notmatch "^https://") {
    throw "$Name must be an HTTPS managed staging URL."
  }
  if ($Value -match "localhost|127\.0\.0\.1|example") {
    throw "$Name must point at real staging infrastructure, not a local or placeholder host."
  }
}

Assert-ManagedUrl "WebBaseUrl" $WebBaseUrl
Assert-ManagedUrl "ApiBaseUrl" $ApiBaseUrl

if ([string]::IsNullOrWhiteSpace($ExpectedCommit) -or $ExpectedCommit -eq "managed-git-sha" -or $ExpectedCommit -eq "local") {
  throw "ExpectedCommit must be the deployed staging git SHA."
}

& "$PSScriptRoot\validate-env.ps1" -Environment staging -EnvFile $EnvFile
& "$PSScriptRoot\smoke-test.ps1" -WebBaseUrl $WebBaseUrl -ApiBaseUrl $ApiBaseUrl
& "$PSScriptRoot\incident-drill-check.ps1" `
  -ApiBaseUrl $ApiBaseUrl `
  -ExpectedStatus UP `
  -ExpectedEnvironment staging `
  -ExpectedCommit $ExpectedCommit `
  -RequireQueueMonitoring:$RequireQueueMonitoring

$base = $ApiBaseUrl.TrimEnd("/")
$web = $WebBaseUrl.TrimEnd("/")

$dashboardResponse = Invoke-WebRequest -Uri "$web/dashboard" -Method Get -TimeoutSec 20
if ($dashboardResponse.StatusCode -lt 200 -or $dashboardResponse.StatusCode -ge 400) {
  throw "Dashboard route returned HTTP $($dashboardResponse.StatusCode)."
}
if ($dashboardResponse.Content -match "NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS|__SPENDSENSE_E2E_SESSION__|ci-lighthouse-trace") {
  throw "Dashboard response contains test bypass markers."
}

$dependencies = Invoke-RestMethod -Uri "$base/api/v1/health/dependencies" -Method Get -TimeoutSec 15
if ($dependencies.data.status -ne "UP") {
  throw "Managed dependency check is not UP: $($dependencies | ConvertTo-Json -Compress)"
}

$metrics = Invoke-RestMethod -Uri "$base/api/v1/health/metrics" -Method Get -TimeoutSec 15
if ([string]::IsNullOrWhiteSpace($metrics.data.status)) {
  throw "Queue and worker metrics did not return an operational status."
}

if ($RequireAdminVerification) {
  if ([string]::IsNullOrWhiteSpace($AdminBearerToken)) {
    throw "AdminBearerToken is required when RequireAdminVerification is set."
  }
  $headers = @{ Authorization = "Bearer $AdminBearerToken" }
  $adminOverview = Invoke-RestMethod -Uri "$base/api/v1/admin/operations/overview" -Headers $headers -Method Get -TimeoutSec 15
  $traceEvents = Invoke-RestMethod -Uri "$base/api/v1/admin/operations/trace-events" -Headers $headers -Method Get -TimeoutSec 15

  if ($null -eq $adminOverview.data) {
    throw "Admin operations overview did not return data."
  }
  if ($null -eq $traceEvents.data) {
    throw "Operational trace events did not return data."
  }
}

Write-Host "Managed staging rollout rehearsal passed for $web and $base at commit $ExpectedCommit."
