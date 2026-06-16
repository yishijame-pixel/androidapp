# 整合云端皮肤动画 → dist/pac_maze_skins（不打进 APK）
# 用法: powershell -File backend/tools/build_pac_maze_skins.ps1 [-DownloadRoot "D:\download"]

param(
    [string]$DownloadRoot = "D:\download"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$SkinsRoot = Join-Path $RepoRoot "dist\pac_maze_skins"

function Ensure-Dir($path) {
    New-Item -ItemType Directory -Force -Path $path | Out-Null
}

function Extract-ZipToDir($zipPath, $destDir, [string]$RenamePrefix = "") {
    if (-not (Test-Path $zipPath)) {
        Write-Host "  skip missing: $zipPath" -ForegroundColor Yellow
        return 0
    }
    Ensure-Dir $destDir
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName.EndsWith("/")) { continue }
            $name = Split-Path $entry.FullName -Leaf
            if (-not $name.ToLower().EndsWith(".png")) { continue }
            if ($RenamePrefix -and $name -match '^image_(\d+)\.png$') {
                $name = "${RenamePrefix}_$($Matches[1]).png"
            }
            $dest = Join-Path $destDir $name
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest, $true)
        }
    } finally {
        $archive.Dispose()
    }
    return (Get-ChildItem $destDir -File).Count
}

Write-Host "`n=== 豆人迷宫云端皮肤包构建 ===" -ForegroundColor Cyan
Write-Host "源: $DownloadRoot"
Write-Host "输出: $SkinsRoot"

if (Test-Path $SkinsRoot) { Remove-Item $SkinsRoot -Recurse -Force }
Ensure-Dir $SkinsRoot

# —— 行走小鸡 Pro Max（多片段）——
$ProMaxRoot = Join-Path $SkinsRoot "food_chick_walker_pro_max"
$ProMaxClips = @("idle", "walk", "run", "jump", "attack", "die")
Write-Host "`n[Pro Max]" -ForegroundColor Yellow
$proMaxManifest = @{ skinId = "food_chick_walker_pro_max"; clips = @{} }
foreach ($clip in $ProMaxClips) {
    $zipPath = Join-Path $DownloadRoot "$clip.zip"
    $clipDir = Join-Path $ProMaxRoot $clip
    $count = Extract-ZipToDir $zipPath $clipDir
    if ($count -gt 0) {
        Write-Host "  $clip : $count frames"
        $proMaxManifest.clips[$clip] = $count
    }
}
$proMaxManifest | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $ProMaxRoot "anim_manifest.json") -Encoding UTF8

# —— 纯行走序列帧皮肤 ——
$WalkOnlySkins = @(
    @{ Folder = "xia_walk";        Zip = "xia_wark.zip";        RenamePrefix = "" },
    @{ Folder = "laoshu_walk";    Zip = "laoshu_walk.zip";    RenamePrefix = "" },
    @{ Folder = "qinting_walk";   Zip = "qinting_walk.zip";   RenamePrefix = "" },
    @{ Folder = "wenzi_walk";     Zip = "wenzi_walk.zip";     RenamePrefix = "" },
    @{ Folder = "toushi_walk";    Zip = "toushi.zip";         RenamePrefix = "walk" }
)

Write-Host "`n[Walk-only skins]" -ForegroundColor Yellow
foreach ($skin in $WalkOnlySkins) {
    $skinRoot = Join-Path $SkinsRoot $skin.Folder
    $walkDir = Join-Path $skinRoot "walk"
    $zipPath = Join-Path $DownloadRoot $skin.Zip
    $count = Extract-ZipToDir $zipPath $walkDir $skin.RenamePrefix
    Write-Host "  $($skin.Folder) : $count frames"
    @{
        skinId = $skin.Folder
        clips = @{ walk = $count }
    } | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $skinRoot "anim_manifest.json") -Encoding UTF8
}

function Write-Preview($skinRoot, $candidates) {
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            Copy-Item $candidate (Join-Path $skinRoot "preview.png") -Force
            return $true
        }
    }
    return $false
}

Write-Host "`n[Preview covers]" -ForegroundColor Yellow
$PreviewSkins = @(
    @{ Root = (Join-Path $SkinsRoot "food_chick_walker_pro_max"); Candidates = @(
        (Join-Path $SkinsRoot "food_chick_walker_pro_max/idle/idle_1.png"),
        (Join-Path $SkinsRoot "food_chick_walker_pro_max/walk/walk_1.png")
    )},
    @{ Root = (Join-Path $SkinsRoot "xia_walk"); Candidates = @((Join-Path $SkinsRoot "xia_walk/walk/walk_1.png")) },
    @{ Root = (Join-Path $SkinsRoot "laoshu_walk"); Candidates = @((Join-Path $SkinsRoot "laoshu_walk/walk/walk_1.png")) },
    @{ Root = (Join-Path $SkinsRoot "qinting_walk"); Candidates = @((Join-Path $SkinsRoot "qinting_walk/walk/walk_1.png")) },
    @{ Root = (Join-Path $SkinsRoot "wenzi_walk"); Candidates = @((Join-Path $SkinsRoot "wenzi_walk/walk/walk_1.png")) },
    @{ Root = (Join-Path $SkinsRoot "toushi_walk"); Candidates = @((Join-Path $SkinsRoot "toushi_walk/walk/walk_1.png")) }
)
foreach ($skin in $PreviewSkins) {
    if (Write-Preview $skin.Root $skin.Candidates) {
        Write-Host "  preview: $($skin.Root | Split-Path -Leaf)"
    } else {
        Write-Host "  missing preview source: $($skin.Root | Split-Path -Leaf)" -ForegroundColor Red
    }
}

$allFiles = Get-ChildItem $SkinsRoot -Recurse -File
$totalMb = [math]::Round(($allFiles | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
Set-Content -Path (Join-Path $SkinsRoot "bundle_version.txt") -Value "7" -Encoding ASCII -NoNewline
Write-Host "`n完成: $($allFiles.Count) 个文件, 合计 $totalMb MB (bundle_version=7)" -ForegroundColor Green
Write-Host "下一步: powershell -File backend/tools/upload_pac_maze_skins.ps1"
