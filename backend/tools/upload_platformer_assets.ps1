#!/usr/bin/env pwsh
# 打包并上传 platformer_characters.zip + asset_manifest.json 到 CloudBase
# 用法: powershell -File backend/tools/upload_platformer_assets.ps1
# 前置: tcb login 且 backend/cloudbaserc.json envId 正确

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$BuildScript = Join-Path $PSScriptRoot 'build_platformer_assets.ps1'
$ZipPath = Join-Path $RepoRoot 'dist\asset-bundles\platformer_characters.zip'
$ManifestLocal = Join-Path $PSScriptRoot 'asset_manifest.json'
$CloudPrefix = 'yishi-assetss/v1/bundles'
$ManifestCloud = 'yishi-assetss/v1/manifest.json'

Write-Host "`n=== [1/3] Build platformer_characters.zip ===" -ForegroundColor Cyan
& $BuildScript
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (-not (Test-Path $ZipPath)) {
    Write-Host "Missing $ZipPath" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== [2/3] Upload bundle ===" -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot 'backend')
$cloudPath = "$CloudPrefix/platformer_characters.zip"
Write-Host "upload platformer_characters.zip -> $cloudPath"
tcb storage upload $ZipPath $cloudPath --times 3
if ($LASTEXITCODE -ne 0) {
    Pop-Location
    Write-Host "Upload failed (run: tcb login)" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== [3/3] Upload manifest.json ===" -ForegroundColor Cyan
Write-Host "upload manifest.json -> $ManifestCloud"
tcb storage upload $ManifestLocal $ManifestCloud --times 3
$code = $LASTEXITCODE
Pop-Location

if ($code -ne 0) {
    Write-Host "Manifest upload failed" -ForegroundColor Red
    exit 1
}

Write-Host "`nUpload OK." -ForegroundColor Green
Write-Host "COS: $CloudPrefix/platformer_characters.zip"
Write-Host "Manifest: https://6675-funlife-prod-d8gxf7og0518b8253-1333176506.tcb.qcloud.la/yishi-assetss/v1/manifest.json"
