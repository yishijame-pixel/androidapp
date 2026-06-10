# 账号恢复全量测试（本地单元 + 可选 E2E）
# 用法（在 backend 目录）:
#   powershell -File tools/run_account_recovery_tests.ps1
#   powershell -File tools/run_account_recovery_tests.ps1 -IncludeE2E
#   powershell -File tools/run_account_recovery_tests.ps1 -IncludeAndroid

param(
    [switch]$IncludeE2E,
    [switch]$IncludeAndroid
)

$ErrorActionPreference = "Stop"
$BackendRoot = Split-Path $PSScriptRoot -Parent
$RepoRoot = Split-Path $BackendRoot -Parent
Push-Location $BackendRoot

Write-Host "`n=== [1] sync-sku ===" -ForegroundColor Cyan
node sync-sku.js
if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }

Write-Host "`n=== [2] account-recover-core unit tests (9 cases) ===" -ForegroundColor Cyan
node functions/account_recover/account_recover.test.js
if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }

if ($IncludeAndroid) {
    Write-Host "`n=== [3] Android unit tests (6 cases) ===" -ForegroundColor Cyan
    Pop-Location
    Push-Location $RepoRoot
    .\gradlew :app:testDebugUnitTest `
        --tests "com.example.funlife.vip.AccountRecoveryLogicTest" `
        --tests "com.example.funlife.repository.AccountRecoveryRepositoryTest" `
        --no-daemon
    if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }
    Pop-Location
    Push-Location $BackendRoot
} else {
    Write-Host "`n=== [3] Android skipped (pass -IncludeAndroid) ===" -ForegroundColor Yellow
}

if ($IncludeE2E) {
    Write-Host "`n=== [4] E2E account_recover (prod) ===" -ForegroundColor Cyan
    node tools/test_account_recover.js
    $e2eExit = $LASTEXITCODE
    if ($e2eExit -eq 2) {
        Write-Host "E2E skipped: cloud function not deployed (exit 2)" -ForegroundColor Yellow
    } elseif ($e2eExit -ne 0) {
        Pop-Location
        exit $e2eExit
    }
} else {
    Write-Host "`n=== [4] E2E skipped (pass -IncludeE2E) ===" -ForegroundColor Yellow
}

Pop-Location
Write-Host "`nAccount recovery test suite finished.`n" -ForegroundColor Green
