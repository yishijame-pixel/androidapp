# 横版 catalog 角色/敌人：逐帧 PNG → sprite sheet → 同步 APK assets + 重打 zip
# 用法: powershell -File backend/tools/repack_platformer_sheets.ps1 [-PruneFrames] [-BundleVersion 2] [-Character adventure_girl]

param(
    [switch]$PruneFrames,
    [int]$BundleVersion = 2,
    [string]$Character = "",
    [string]$PlatformerRoot = "",
    [string]$ApkBundleRoot = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $PlatformerRoot) { $PlatformerRoot = Join-Path $RepoRoot "dist\platformer" }
if (-not $ApkBundleRoot) { $ApkBundleRoot = Join-Path $RepoRoot "app\src\main\assets\platformer\platformer_characters" }
$PackScript = Join-Path $PSScriptRoot "pack_sprite_sheets.py"
$BuildScript = Join-Path $PSScriptRoot "build_platformer_assets.ps1"

function Pack-Dir($Dir) {
    if (-not (Test-Path (Join-Path $Dir "anim_manifest.json"))) { return }
    $packArgs = @($PackScript)
    if ($PruneFrames) { $packArgs += "--prune-frames" }
    $packArgs += $Dir
    python @packArgs
    if ($LASTEXITCODE -ne 0) { throw "pack failed: $Dir" }
}

Write-Host "`n=== Pack platformer sprite sheets ===" -ForegroundColor Cyan
if ($Character) {
    $charDir = Join-Path $PlatformerRoot "characters\$Character"
    if (-not (Test-Path $charDir)) { Write-Error "Missing $charDir" }
    Pack-Dir $charDir
} else {
    foreach ($sub in @("characters", "enemies")) {
        $root = Join-Path $PlatformerRoot $sub
        if (-not (Test-Path $root)) { continue }
        Get-ChildItem $root -Directory | ForEach-Object { Pack-Dir $_.FullName }
    }
}

Write-Host "`n=== Sync to APK assets ===" -ForegroundColor Cyan
if (Test-Path $ApkBundleRoot) { Remove-Item $ApkBundleRoot -Recurse -Force }
New-Item -ItemType Directory -Force -Path $ApkBundleRoot | Out-Null
Copy-Item "$PlatformerRoot\*" $ApkBundleRoot -Recurse -Force
Set-Content -Path (Join-Path $ApkBundleRoot "bundle_version.txt") -Value $BundleVersion -Encoding ASCII -NoNewline

Write-Host "`n=== Build platformer_characters.zip ===" -ForegroundColor Cyan
& $BuildScript
if ($LASTEXITCODE -ne 0) { throw "build_platformer_assets failed" }

Write-Host "`nDone. bundle_version=$BundleVersion" -ForegroundColor Green
Write-Host "Push to device: adb push dist\asset-bundles\platformer_characters.zip ... (or reinstall APK)" -ForegroundColor Cyan
