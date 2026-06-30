# 将本地 platformer_sfx 推入已连接设备的 resource_cache。
# 用法: powershell -File backend/tools/push_platformer_sfx_local.ps1 [-Package com.example.funlife]

param(
    [string]$Package = "com.example.funlife",
    [string]$SfxRoot = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $SfxRoot) { $SfxRoot = Join-Path $RepoRoot "dist\platformer_sfx" }

if (-not (Test-Path (Join-Path $SfxRoot "bundle_version.txt"))) {
    Write-Error "Missing $SfxRoot — run import_supertux_platformer.py first."
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { Write-Error "adb not found in PATH" }
$devices = & adb devices | Select-String "device$"
if (-not $devices) { Write-Error "No adb device connected" }

$stagingRemote = "/data/local/tmp/_platformer_sfx_push"

Write-Host "Stage -> $stagingRemote" -ForegroundColor Cyan
& adb shell "rm -rf $stagingRemote && mkdir -p $stagingRemote"
& adb push "$SfxRoot/." "$stagingRemote/"
if ($LASTEXITCODE -ne 0) { throw "adb push failed" }

Write-Host "Install into app cache via run-as..." -ForegroundColor Cyan
$installCmd = @"
run-as $Package sh -c 'rm -rf files/resource_cache/platformer_sfx && mkdir -p files/resource_cache/platformer_sfx && cp -r $stagingRemote/. files/resource_cache/platformer_sfx/'
"@
& adb shell $installCmd
if ($LASTEXITCODE -ne 0) {
    Write-Warning "run-as copy failed — install debug APK and retry."
} else {
    & adb shell "rm -rf $stagingRemote"
}

$bv = Get-Content (Join-Path $SfxRoot "bundle_version.txt") -Raw
Write-Host "`nDone. bundle_version=$bv" -ForegroundColor Green
Write-Host "验证: 杀 App 重进 → 横版冒险 → 任意关卡应有跳跃/BGM 音效"
