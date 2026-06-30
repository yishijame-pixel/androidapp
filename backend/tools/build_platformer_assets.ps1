#!/usr/bin/env pwsh
# 打包 platformer_characters → dist/asset-bundles/platformer_characters.zip
# 用法: powershell -File backend/tools/build_platformer_assets.ps1
# 前置: python backend/tools/platformer_catalog/import_platformer_catalog.py

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$SrcDir = Join-Path $RepoRoot 'app\src\main\assets\platformer\platformer_characters'
$FallbackDir = Join-Path $RepoRoot 'dist\platformer'
$OutDir = Join-Path $RepoRoot 'dist\asset-bundles'
$ZipPath = Join-Path $OutDir 'platformer_characters.zip'
$StagingParent = Join-Path $RepoRoot 'dist\_zip_staging'
$StagingBundle = Join-Path $StagingParent 'platformer_characters'

if (-not (Test-Path $SrcDir)) {
    if (-not (Test-Path $FallbackDir)) {
        Write-Error "Missing bundle source. Run: python backend/tools/platformer_catalog/import_platformer_catalog.py"
    }
    Write-Host "APK mirror missing; staging from dist/platformer" -ForegroundColor Yellow
    $Source = $FallbackDir
} else {
    $Source = $SrcDir
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
if (Test-Path $StagingParent) { Remove-Item $StagingParent -Recurse -Force }
New-Item -ItemType Directory -Force -Path $StagingBundle | Out-Null
Copy-Item "$Source\*" $StagingBundle -Recurse -Force

if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
Compress-Archive -Path $StagingBundle -DestinationPath $ZipPath -CompressionLevel Optimal
Remove-Item $StagingParent -Recurse -Force -ErrorAction SilentlyContinue

$mb = [math]::Round((Get-Item $ZipPath).Length / 1MB, 2)
Write-Host "Built $ZipPath ($mb MB)" -ForegroundColor Green
