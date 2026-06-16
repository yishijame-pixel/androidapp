# 整合 d:\download 音效 → dist/pac_maze_sfx（不打进 APK）
# 用法: powershell -File backend/tools/build_pac_maze_sfx.ps1 [-DownloadRoot "D:\download"]

param(
    [string]$DownloadRoot = "D:\download"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$OutRoot = Join-Path $RepoRoot "dist\pac_maze_sfx"
$ToolsRoot = Join-Path $PSScriptRoot "pac_maze_sfx"

function Ensure-Dir($path) {
    New-Item -ItemType Directory -Force -Path $path | Out-Null
}

function Copy-IfExists($src, $dst) {
    if (-not (Test-Path $src)) {
        Write-Warning "  skip missing: $src"
        return $false
    }
    Ensure-Dir (Split-Path $dst -Parent)
    Copy-Item -LiteralPath $src -Destination $dst -Force
    $kb = [math]::Round((Get-Item $dst).Length / 1KB, 1)
    Write-Host "  -> $(Split-Path $dst -Leaf) ($kb KB)"
    return $true
}

function Find-DownloadFile($pattern) {
    Get-ChildItem -LiteralPath $DownloadRoot -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like $pattern } |
        Select-Object -First 1 -ExpandProperty FullName
}

Write-Host "`n=== 豆人迷宫音效包构建 ===" -ForegroundColor Cyan
Write-Host "源: $DownloadRoot"
Write-Host "输出: $OutRoot"

if (Test-Path $OutRoot) { Remove-Item $OutRoot -Recurse -Force }
Ensure-Dir $OutRoot

# 文档与 manifest
Copy-Item (Join-Path $ToolsRoot "README.md") (Join-Path $OutRoot "README.md") -Force
Copy-Item (Join-Path $ToolsRoot "sfx_manifest.template.json") (Join-Path $OutRoot "sfx_manifest.json") -Force

$kenneyRoot = Join-Path $DownloadRoot "kenney_impact-sounds"
if (Test-Path $kenneyRoot) {
    $kenneyLicense = Join-Path $kenneyRoot "License.txt"
    if (Test-Path $kenneyLicense) {
        Copy-Item $kenneyLicense (Join-Path $OutRoot "LICENSE.txt") -Force
    }
    $kenneyOut = Join-Path $OutRoot "kenney_impact"
    Ensure-Dir $kenneyOut
    $audioSrc = Join-Path $kenneyRoot "Audio"
    if (Test-Path $audioSrc) {
        Copy-Item "$audioSrc\*.ogg" $kenneyOut -Force
        $count = (Get-ChildItem $kenneyOut -File).Count
        Write-Host "  kenney_impact: $count ogg files"
    }
} else {
    Write-Warning "kenney_impact-sounds not found under $DownloadRoot"
}

Write-Host "`n--- curated（默认事件音）---" -ForegroundColor Yellow
$curated = Join-Path $OutRoot "curated"
Ensure-Dir $curated

$curatedMap = @(
    @{ Dst = "checkpoint.wav";     Src = { Find-DownloadFile "Pickup_coin 4.wav" } },
    @{ Dst = "pellet.wav";         Src = { Find-DownloadFile "Pickup_coin 2.wav" } },
    @{ Dst = "power_pellet.wav";   Src = { Find-DownloadFile "Pickup_coin 8.wav" } },
    @{ Dst = "item_pickup.wav";    Src = { Find-DownloadFile "Pickup_coin 3.wav" } },
    @{ Dst = "laser.wav";          Src = { Find-DownloadFile "Laser_shoot 4.wav" } },
    @{ Dst = "hurt.wav";           Src = { Join-Path $DownloadRoot "Explosion 1.wav" } },
    @{ Dst = "ghost_hit.wav";      Src = { Join-Path $DownloadRoot "Explosion 1.wav" } },
    @{ Dst = "bgm_campaign.mp3";   Src = { Join-Path $DownloadRoot "Cute_arcade_gameplay_#4-1781180159174.mp3" } },
    @{ Dst = "bgm_endless.mp3";    Src = { Join-Path $DownloadRoot "Cute_arcade_gameplay_#3-1781180153202.mp3" } },
    @{ Dst = "level_clear.mp3";    Src = { Find-DownloadFile "Victory_fanfare,_cut_#1*.mp3" } },
    @{ Dst = "game_over.mp3";      Src = { Find-DownloadFile "Cute_failure_sound,__#1*.mp3" } },
    @{ Dst = "attack.mp3";         Src = { Find-DownloadFile "Cute_projectile_shoo_#1*.mp3" } },
    @{ Dst = "bgm_gameplay.mp3";   Src = { Find-DownloadFile "Cute_arcade_gameplay_#1-1781180127586.mp3" } },
    @{ Dst = "gate_phase.ogg";     Src = {
            $p = Join-Path $kenneyRoot "Audio\impactBell_heavy_000.ogg"
            if (Test-Path $p) { $p } else { $null }
        }
    }
)

foreach ($item in $curatedMap) {
    $src = & $item.Src
    if ($src) {
        Copy-IfExists $src (Join-Path $curated $item.Dst)
    } else {
        Write-Warning "  curated missing source for $($item.Dst)"
    }
}

Write-Host "`n--- curated/ui（Kenney UI Audio · 固定映射）---" -ForegroundColor Yellow
$uiSrcRoot = Join-Path $DownloadRoot "kenney_ui-audio\Audio"
$uiOut = Join-Path $curated "ui"
Ensure-Dir $uiOut
$uiMap = @(
    @{ Dst = "back.ogg";             Src = "switch2.ogg" },
    @{ Dst = "nav_forward.ogg";      Src = "click1.ogg" },
    @{ Dst = "primary_confirm.ogg";  Src = "switch14.ogg" },
    @{ Dst = "secondary.ogg";        Src = "click3.ogg" },
    @{ Dst = "chip.ogg";             Src = "click2.ogg" },
    @{ Dst = "utility.ogg";          Src = "switch10.ogg" },
    @{ Dst = "tab.ogg";              Src = "switch1.ogg" },
    @{ Dst = "list_select.ogg";      Src = "click4.ogg" },
    @{ Dst = "grid_select.ogg";      Src = "click5.ogg" },
    @{ Dst = "series_card.ogg";       Src = "switch6.ogg" },
    @{ Dst = "mode_featured.ogg";    Src = "switch18.ogg" },
    @{ Dst = "mode_option.ogg";      Src = "switch22.ogg" },
    @{ Dst = "map_node.ogg";         Src = "switch5.ogg" },
    @{ Dst = "map_chip.ogg";         Src = "click3.ogg" },
    @{ Dst = "toggle.ogg";           Src = "switch8.ogg" }
)
foreach ($item in $uiMap) {
    $src = Join-Path $uiSrcRoot $item.Src
    Copy-IfExists $src (Join-Path $uiOut $item.Dst)
}
$kenneyUiOut = Join-Path $OutRoot "kenney_ui"
if (Test-Path $uiSrcRoot) {
    Ensure-Dir $kenneyUiOut
    Copy-Item "$uiSrcRoot\*.ogg" $kenneyUiOut -Force
    $uiCount = (Get-ChildItem $kenneyUiOut -File).Count
    Write-Host "  kenney_ui: $uiCount ogg files (full library)"
} else {
    Write-Warning "kenney_ui-audio not found under $DownloadRoot"
}

Write-Host "`n--- variants ---" -ForegroundColor Yellow
$variantGroups = @(
    @{ Dir = "variants\victory";    Pattern = "Victory_fanfare,_cut_#*.mp3";    Prefix = "victory" },
    @{ Dir = "variants\failure";   Pattern = "Cute_failure_sound,__#*.mp3";    Prefix = "failure" },
    @{ Dir = "variants\projectile"; Pattern = "Cute_projectile_shoo_#*.mp3";  Prefix = "projectile" },
    @{ Dir = "variants\gameplay";   Pattern = "Cute_arcade_gameplay_#*";        Prefix = "gameplay" }
)

foreach ($g in $variantGroups) {
    $dir = Join-Path $OutRoot $g.Dir
    Ensure-Dir $dir
    $files = Get-ChildItem -LiteralPath $DownloadRoot -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like $g.Pattern } |
        Sort-Object Name
    $i = 1
    foreach ($f in $files) {
        $ext = $f.Extension
        Copy-IfExists $f.FullName (Join-Path $dir ("{0}_{1:D2}{2}" -f $g.Prefix, $i, $ext))
        $i++
    }
}

Write-Host "`n--- retro（原始 wav / jfxr）---" -ForegroundColor Yellow
$retro = Join-Path $OutRoot "retro"
Ensure-Dir $retro
@(
    "Pickup_coin 2.wav", "Pickup_coin 3.wav", "Pickup_coin 4.wav", "Pickup_coin 8.wav",
    "Hit_hurt 1.wav", "Hit_hurt 1 (1).wav", "Laser_shoot 4.wav", "Powerup 1.jfxr"
) | ForEach-Object {
    $src = Join-Path $DownloadRoot $_
    if (Test-Path $src) {
        Copy-IfExists $src (Join-Path $retro $_)
    }
}
$procDir = Join-Path $retro "procedural"
Ensure-Dir $procDir
$jfxr = Join-Path $DownloadRoot "Powerup 1.jfxr"
if (Test-Path $jfxr) { Copy-IfExists $jfxr (Join-Path $procDir "powerup.jfxr") }

# 统计
$allFiles = Get-ChildItem $OutRoot -Recurse -File
$totalMb = [math]::Round(($allFiles | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
Write-Host "`n完成: $($allFiles.Count) 个文件, 合计 $totalMb MB" -ForegroundColor Green
Write-Host "下一步: powershell -File backend/tools/upload_pac_maze_sfx.ps1"
