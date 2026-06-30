# 将本地 pac_maze_skins v22 推入已连接设备的 resource_cache，免 CDN 验证 sprite sheet。
# 用法: powershell -File backend/tools/push_pac_maze_skins_local.ps1 [-Package com.example.funlife]

param(
    [string]$Package = "com.example.funlife",
    [string]$SkinsRoot = "",
    [switch]$ClearDecodedCache
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $SkinsRoot) { $SkinsRoot = Join-Path $RepoRoot "dist\pac_maze_skins" }

if (-not (Test-Path (Join-Path $SkinsRoot "bundle_version.txt"))) {
    Write-Error "Missing $SkinsRoot — run repack_pac_maze_sheets.ps1 first."
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { Write-Error "adb not found in PATH" }
$devices = & adb devices | Select-String "device$"
if (-not $devices) { Write-Error "No adb device connected" }

$remoteRoot = "/data/data/$Package/files/resource_cache/pac_maze_skins"
$stagingRemote = "/data/local/tmp/_pac_maze_skins_push"

Write-Host "Stage -> $stagingRemote" -ForegroundColor Cyan
& adb shell "rm -rf $stagingRemote && mkdir -p $stagingRemote"
& adb push "$SkinsRoot/." "$stagingRemote/"
if ($LASTEXITCODE -ne 0) { throw "adb push to sdcard failed" }

Write-Host "Install into app cache via run-as..." -ForegroundColor Cyan
$installCmd = @"
run-as $Package sh -c 'rm -rf files/resource_cache/pac_maze_skins && mkdir -p files/resource_cache/pac_maze_skins && cp -r $stagingRemote/. files/resource_cache/pac_maze_skins/'
"@
& adb shell $installCmd
if ($LASTEXITCODE -ne 0) {
    Write-Warning "run-as copy failed — install debug APK and retry, or upload CDN zip v22."
} else {
    if ($ClearDecodedCache) {
        & adb shell "run-as $Package sh -c 'rm -rf files/resource_cache/decoded_pac_maze_skins'"
        Write-Host "Cleared decoded_pac_maze_skins" -ForegroundColor Yellow
    }
    & adb shell "rm -rf $stagingRemote"
}

$bv = Get-Content (Join-Path $SkinsRoot "bundle_version.txt") -Raw
Write-Host "`nDone. bundle_version=$bv" -ForegroundColor Green
Write-Host "验证: 杀 App 重进 → 豆人迷宫 → 横版冒险 → 行走小鸡 Pro Max"
Write-Host "Logcat: adb logcat -s PacMazeRemoteSkinAnim ResourceStore"
