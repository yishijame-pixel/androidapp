# 将本地 platformer_supertux 推入已连接设备的 resource_cache。
# 用法: powershell -File backend/tools/push_platformer_supertux_local.ps1 [-Package com.example.funlife]

param(
    [string]$Package = "com.example.funlife",
    [string]$BundleRoot = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $BundleRoot) { $BundleRoot = Join-Path $RepoRoot "dist\platformer_supertux" }

if (-not (Test-Path (Join-Path $BundleRoot "content_catalog.json"))) {
    Write-Error "Missing $BundleRoot — run import_supertux_platformer.py first."
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { Write-Error "adb not found in PATH" }
$devices = & adb devices | Select-String "device$"
if (-not $devices) { Write-Error "No adb device connected" }

$stagingRemote = "/data/local/tmp/_platformer_supertux_push"

Write-Host "Stage -> $stagingRemote" -ForegroundColor Cyan
& adb shell "rm -rf $stagingRemote && mkdir -p $stagingRemote"
& adb push "$BundleRoot/." "$stagingRemote/"
if ($LASTEXITCODE -ne 0) { throw "adb push failed" }

Write-Host "Install into app cache via run-as..." -ForegroundColor Cyan
$installCmd = @"
run-as $Package sh -c 'rm -rf files/resource_cache/platformer_supertux && mkdir -p files/resource_cache/platformer_supertux && cp -r $stagingRemote/. files/resource_cache/platformer_supertux/'
"@
& adb shell $installCmd
if ($LASTEXITCODE -ne 0) {
    Write-Warning "run-as copy failed — install debug APK and retry."
} else {
    & adb shell "rm -rf $stagingRemote"
}

$bv = Get-Content (Join-Path $BundleRoot "bundle_version.txt") -Raw
Write-Host "`nDone. bundle_version=$bv" -ForegroundColor Green
Write-Host "验证: 横版冒险 → 选关「南极 901–910」→ 冰雪地砖与 SuperTux 关卡"
