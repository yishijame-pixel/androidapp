# 将行走小鸡 Pro Max 最小可玩 sheet 打入 APK assets，离线可验证 sprite sheet 加载。
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Src = Join-Path $Root "dist\pac_maze_skins\food_chick_walker_pro_max"
$Dst = Join-Path $Root "app\src\main\assets\pac_maze_skins\food_chick_walker_pro_max"

if (-not (Test-Path (Join-Path $Src "anim_manifest.json"))) {
    Write-Error "Missing source skin at $Src — run repack_pac_maze_sheets.ps1 first."
}

$manifest = Get-Content (Join-Path $Src "anim_manifest.json") -Raw | ConvertFrom-Json
$walkSheet = $manifest.clips.walk.sheet.file
$jumpSheet = $manifest.clips.jump.sheet.file
if (-not $walkSheet -or -not $jumpSheet) {
    Write-Error "Manifest missing sheet metadata — run pack_sprite_sheets first."
}

if (Test-Path $Dst) { Remove-Item $Dst -Recurse -Force }
New-Item -ItemType Directory -Force -Path (Join-Path $Dst "walk"), (Join-Path $Dst "jump") | Out-Null
Copy-Item (Join-Path $Src "anim_manifest.json") $Dst -Force
if (Test-Path (Join-Path $Src "preview.png")) {
    Copy-Item (Join-Path $Src "preview.png") $Dst -Force
}
Copy-Item (Join-Path $Src "walk\$walkSheet") (Join-Path $Dst "walk\") -Force
Copy-Item (Join-Path $Src "jump\$jumpSheet") (Join-Path $Dst "jump\") -Force

$bytes = (Get-ChildItem -Recurse $Dst -File | Measure-Object -Property Length -Sum).Sum
Write-Host "Bootstrap sheets copied to assets ($([math]::Round($bytes / 1MB, 2)) MB)" -ForegroundColor Green
Write-Host "  walk/$walkSheet"
Write-Host "  jump/$jumpSheet"
