#!/usr/bin/env pwsh
# 从 backup_platformer_pre_sheet.ps1 生成的目录回退资源与代码。
# 用法: powershell -File backend/tools/restore_platformer_pre_sheet.ps1 [-BackupRoot path]

param(
    [string]$BackupRoot = ""
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $BackupRoot) {
    $LatestFile = Join-Path $RepoRoot 'backup\LATEST_PLATFORMER_PRE_SHEET.txt'
    if (-not (Test-Path $LatestFile)) {
        Write-Error "No backup path. Run backup_platformer_pre_sheet.ps1 first or pass -BackupRoot."
    }
    $BackupRoot = (Get-Content $LatestFile -Raw).Trim()
}

if (-not (Test-Path $BackupRoot)) {
    Write-Error "Backup not found: $BackupRoot"
}

function Restore-Tree($RelSrc, $RelDst) {
    $src = Join-Path $BackupRoot $RelSrc
    if (-not (Test-Path $src)) { return }
    $dst = Join-Path $RepoRoot $RelDst
    if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
    $parent = Split-Path $dst -Parent
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    Copy-Item $src $dst -Recurse -Force
    Write-Host "  restored: $RelDst" -ForegroundColor Green
}

Write-Host "`n=== Restore from $BackupRoot ===" -ForegroundColor Cyan
Write-Host "[Resources]" -ForegroundColor Yellow
Restore-Tree 'dist\platformer' 'dist\platformer'
Restore-Tree 'dist\pac_maze_skins' 'dist\pac_maze_skins'
Restore-Tree 'dist\asset-bundles\platformer_characters.zip' 'dist\asset-bundles\platformer_characters.zip'
Restore-Tree 'dist\asset-bundles\pac_maze_skins.zip' 'dist\asset-bundles\pac_maze_skins.zip'
Restore-Tree 'app\assets\platformer\platformer_characters' 'app\src\main\assets\platformer\platformer_characters'
Restore-Tree 'app\assets\pac_maze_skins\food_chick_walker_pro_max' 'app\src\main\assets\pac_maze_skins\food_chick_walker_pro_max'

Write-Host "`n[Kotlin sources]" -ForegroundColor Yellow
$CodeSrc = Join-Path $BackupRoot 'code'
if (Test-Path $CodeSrc) {
    Get-ChildItem $CodeSrc -Recurse -File | ForEach-Object {
        $rel = $_.FullName.Substring($CodeSrc.Length + 1)
        $dst = Join-Path $RepoRoot ($rel -replace '/', '\')
        $dstDir = Split-Path $dst -Parent
        New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
        Copy-Item $_.FullName $dst -Force
        Write-Host "  restored: $rel" -ForegroundColor Green
    }
}

Write-Host "`nRestore complete. Rebuild APK if needed: .\gradlew :app:assembleDebug" -ForegroundColor Green
