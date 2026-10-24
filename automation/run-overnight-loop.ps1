param(
    [int]$MaxIterations = 10,
    [switch]$DryRun
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Chorus Music: Autonomous Overnight Test & Repair Pipeline" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$TaskManifestPath = "$PSScriptRoot\task-manifest.json"
if (-not (Test-Path $TaskManifestPath)) {
    Write-Error "Task manifest not found at $TaskManifestPath"
    exit 1
}

$manifest = Get-Content $TaskManifestPath -Raw | ConvertFrom-Json
Write-Host "Loaded task manifest for project: $($manifest.project)" -ForegroundColor Green
Write-Host "Target Application ID: $($manifest.applicationId)" -ForegroundColor Green

if ($DryRun) {
    Write-Host "[DRY RUN] Validating manifest test suites..." -ForegroundColor Yellow
    foreach ($suite in $manifest.testSuites) {
        Write-Host " - Test Suite: $($suite.name) [Priority: $($suite.priority), Enabled: $($suite.enabled)]" -ForegroundColor Gray
    }
    Write-Host "[DRY RUN] Completed successfully." -ForegroundColor Green
    exit 0
}

Write-Host "Starting build verification using $($manifest.remediation.gradleVerifyTask)..." -ForegroundColor Yellow
$compileOutput = & .\gradlew $manifest.remediation.gradleVerifyTask
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation check failed during overnight loop execution."
    exit $LASTEXITCODE
}

Write-Host "All build and theme verifications PASSED." -ForegroundColor Green
