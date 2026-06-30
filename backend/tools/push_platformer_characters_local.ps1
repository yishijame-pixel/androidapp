# 将本地 platformer_characters v2 推入已连接设备的 resource_cache，免 CDN 校验失败。
# 用法: powershell -File backend/tools/push_platformer_characters_local.ps1 [-Package com.example.funlife]

param(
    [string]$Package = "com.example.funlife",
    [string]$ZipPath = "",
    [switch]$ClearDecodedCache
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $ZipPath) { $ZipPath = Join-Path $RepoRoot "dist\asset-bundles\platformer_characters.zip" }

if (-not (Test-Path $ZipPath)) {
    Write-Error "Missing $ZipPath — run: powershell -File backend/tools/repack_platformer_sheets.ps1 -PruneFrames -BundleVersion 2"
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { Write-Error "adb not found in PATH" }
$devices = & adb devices | Select-String "device$"
if (-not $devices) { Write-Error "No adb device connected" }

$stagingLocal = Join-Path $RepoRoot "dist\_push_platformer_staging"
$bundleLocal = Join-Path $stagingLocal "platformer_characters"
if (Test-Path $stagingLocal) { Remove-Item $stagingLocal -Recurse -Force }
New-Item -ItemType Directory -Force -Path $bundleLocal | Out-Null
Expand-Archive -Path $ZipPath -DestinationPath $stagingLocal -Force

if (-not (Test-Path (Join-Path $bundleLocal "bundle_version.txt"))) {
    Write-Error "Zip layout invalid — expected platformer_characters/bundle_version.txt"
}

$remoteRoot = "/data/data/$Package/files/resource_cache/platformer_characters"
$stagingRemote = "/data/local/tmp/_platformer_characters_push"

Write-Host "Stage -> $stagingRemote" -ForegroundColor Cyan
& adb shell "rm -rf $stagingRemote && mkdir -p $stagingRemote"
& adb push "$bundleLocal/." "$stagingRemote/"
if ($LASTEXITCODE -ne 0) { throw "adb push failed" }

Write-Host "Install into app cache via run-as..." -ForegroundColor Cyan
$installCmd = @"
run-as $Package sh -c 'rm -rf files/resource_cache/platformer_characters && mkdir -p files/resource_cache/platformer_characters && cp -r $stagingRemote/. files/resource_cache/platformer_characters/'
"@
& adb shell $installCmd
if ($LASTEXITCODE -ne 0) {
    Write-Warning "run-as copy failed — install debug APK and retry, or upload CDN zip v2."
} else {
    if ($ClearDecodedCache) {
        & adb shell "run-as $Package sh -c 'rm -rf files/resource_cache/decoded_platformer_characters'"
        Write-Host "Cleared decoded_platformer_characters" -ForegroundColor Yellow
    }
    & adb shell "rm -rf $stagingRemote"
}

$bv = Get-Content (Join-Path $bundleLocal "bundle_version.txt") -Raw
Remove-Item $stagingLocal -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "`nDone. bundle_version=$bv" -ForegroundColor Green
Write-Host "验证: 杀 App 重进 → 横幅应显示横版冒险资源已就绪"
Write-Host "Logcat: adb logcat -s ResourceStore PlatformerRemoteAnim"
