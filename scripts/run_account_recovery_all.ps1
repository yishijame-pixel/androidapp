# 仓库根目录：账号恢复一键测试（后端 + Android + 可选 E2E）
# 用法:
#   powershell -File scripts/run_account_recovery_all.ps1
#   powershell -File scripts/run_account_recovery_all.ps1 -IncludeE2E

param(
    [switch]$IncludeE2E
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path $PSScriptRoot -Parent
Push-Location $RepoRoot

Write-Host "`n========== Account Recovery Full Test Suite ==========" -ForegroundColor Cyan

$args = @("-IncludeAndroid")
if ($IncludeE2E) { $args += "-IncludeE2E" }

& powershell -File backend/tools/run_account_recovery_tests.ps1 @args

$exit = $LASTEXITCODE
Pop-Location
exit $exit
