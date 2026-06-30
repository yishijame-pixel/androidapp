#!/usr/bin/env pwsh
# 备份横版 / PacMaze 资源与相关 Kotlin，便于 sheet 改造前回退。
# 用法: powershell -File backend/tools/backup_platformer_pre_sheet.ps1
# 回退: powershell -File backend/tools/restore_platformer_pre_sheet.ps1

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$BackupRoot = Join-Path $RepoRoot "backup\platformer-pre-sheet-$Stamp"

function Copy-IfExists($Src, $Dst) {
    if (-not (Test-Path $Src)) {
        Write-Host "  skip (missing): $Src" -ForegroundColor DarkGray
        return
    }
    $parent = Split-Path $Dst -Parent
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    Copy-Item $Src $Dst -Recurse -Force
    Write-Host "  ok: $Src" -ForegroundColor Green
}

Write-Host "`n=== Backup platformer pre-sheet -> $BackupRoot ===" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $BackupRoot | Out-Null

Write-Host "`n[Resources]" -ForegroundColor Yellow
Copy-IfExists (Join-Path $RepoRoot 'dist\platformer') (Join-Path $BackupRoot 'dist\platformer')
Copy-IfExists (Join-Path $RepoRoot 'dist\pac_maze_skins') (Join-Path $BackupRoot 'dist\pac_maze_skins')
Copy-IfExists (Join-Path $RepoRoot 'dist\asset-bundles\platformer_characters.zip') (Join-Path $BackupRoot 'dist\asset-bundles\platformer_characters.zip')
Copy-IfExists (Join-Path $RepoRoot 'dist\asset-bundles\pac_maze_skins.zip') (Join-Path $BackupRoot 'dist\asset-bundles\pac_maze_skins.zip')
Copy-IfExists (Join-Path $RepoRoot 'app\src\main\assets\platformer\platformer_characters') (Join-Path $BackupRoot 'app\assets\platformer\platformer_characters')
Copy-IfExists (Join-Path $RepoRoot 'app\src\main\assets\pac_maze_skins\food_chick_walker_pro_max') (Join-Path $BackupRoot 'app\assets\pac_maze_skins\food_chick_walker_pro_max')

Write-Host "`n[Kotlin sources]" -ForegroundColor Yellow
$CodeFiles = @(
    'app\src\main\java\com\example\funlife\game\platformer\PlatformerRenderer.kt',
    'app\src\main\java\com\example\funlife\game\platformer\PlatformerPlayerSprites.kt',
    'app\src\main\java\com\example\funlife\game\platformer\PlatformerSpriteDraw.kt',
    'app\src\main\java\com\example\funlife\game\platformer\PlatformerCharacters.kt',
    'app\src\main\java\com\example\funlife\game\platformer\catalog\PlatformerRemoteAnimCache.kt',
    'app\src\main\java\com\example\funlife\game\platformer\catalog\PlatformerSkinRenderer.kt',
    'app\src\main\java\com\example\funlife\game\platformer\catalog\PlatformerSpriteAtlasCache.kt',
    'app\src\main\java\com\example\funlife\game\platformer\catalog\PlatformerCharacterPrefetch.kt',
    'app\src\main\java\com\example\funlife\ui\screens\platformer\PlatformerBootLoader.kt',
    'app\src\main\java\com\example\funlife\ui\screens\platformer\PlatformerBootCache.kt',
    'app\src\main\java\com\example\funlife\ui\screens\platformer\PlatformerScreen.kt',
    'app\src\main\java\com\example\funlife\ui\screens\platformer\PlatformerLoadingScreen.kt',
    'app\src\main\java\com\example\funlife\ui\screens\pacmaze\cosmetic\skin\PacMazeRemoteSkinAnimCache.kt',
    'app\src\main\java\com\example\funlife\ui\screens\pacmaze\cosmetic\skin\PacMazeSkinSheetPlayback.kt',
    'app\src\main\java\com\example\funlife\ui\screens\pacmaze\cosmetic\skin\PacMazeSkinAnimManifest.kt',
    'app\src\main\java\com\example\funlife\resource\ResourceStore.kt',
    'backend\tools\build_platformer_assets.ps1',
    'backend\tools\build_pac_maze_skins.ps1',
    'backend\tools\pack_sprite_sheets.py',
    'backend\tools\repack_pac_maze_sheets.ps1'
)
$CodeDst = Join-Path $BackupRoot 'code'
New-Item -ItemType Directory -Force -Path $CodeDst | Out-Null
foreach ($rel in $CodeFiles) {
    $src = Join-Path $RepoRoot $rel
    if (-not (Test-Path $src)) { continue }
    $dst = Join-Path $CodeDst ($rel -replace '\\', '/')
    $dstDir = Split-Path $dst -Parent
    New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
    Copy-Item $src $dst -Force
}

$gitHead = try { git -C $RepoRoot rev-parse HEAD 2>$null } catch { $null }
$meta = @{
    stamp       = $Stamp
    backupRoot  = $BackupRoot
    gitHead     = $gitHead
    note        = 'Restore with backend/tools/restore_platformer_pre_sheet.ps1 -BackupRoot <this folder>'
} | ConvertTo-Json -Depth 3
$meta | Set-Content (Join-Path $BackupRoot 'BACKUP_INFO.json') -Encoding UTF8

# 记录最新备份路径，供 restore 默认使用
$LatestFile = Join-Path $RepoRoot 'backup\LATEST_PLATFORMER_PRE_SHEET.txt'
New-Item -ItemType Directory -Force -Path (Split-Path $LatestFile -Parent) | Out-Null
$BackupRoot | Set-Content $LatestFile -Encoding UTF8

Write-Host "`nBackup complete: $BackupRoot" -ForegroundColor Green
Write-Host "Restore: powershell -File backend/tools/restore_platformer_pre_sheet.ps1" -ForegroundColor Cyan
