# 快速安装 Debug 包到已连接手机（约 30 秒，需 FAST_DEV_INSTALL=true）
# 用法: powershell -File scripts/install_fast.ps1

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path $PSScriptRoot -Parent
Push-Location $RepoRoot

$props = Join-Path $RepoRoot "local.properties"
if (-not (Select-String -Path $props -Pattern "FAST_DEV_INSTALL=true" -Quiet)) {
    Write-Host "提示: 在 local.properties 添加 FAST_DEV_INSTALL=true 可大幅缩小 APK" -ForegroundColor Yellow
}

Write-Host "`n=== 编译 + 安装（精简 debug 包）===" -ForegroundColor Cyan
.\gradlew :app:installDebug --no-daemon
$code = $LASTEXITCODE
Pop-Location
if ($code -ne 0) { exit $code }
Write-Host "`n安装完成。" -ForegroundColor Green
