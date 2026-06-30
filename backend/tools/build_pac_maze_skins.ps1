# 整合云端皮肤动画 → dist/pac_maze_skins（不打进 APK）
# 用法: powershell -File backend/tools/build_pac_maze_skins.ps1 [-DownloadRoot "D:\download"]

param(
    [string]$DownloadRoot = "D:\download",
    [string]$YishiRoot = "C:\Users\Administrator\Desktop\yishi",
    [string]$LongWalkPipelineRoot = "D:\yishi\0483a2de6d1b4301b40ef283707a08e0",
    [switch]$YishiOnly
)

$BUNDLE_VERSION = "25"

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
            } elseif ($name -match '^matte_(\d+)\.png$') {
                $num = [int]$Matches[1]
                $name = "walk_$num.png"
            }
            $dest = Join-Path $destDir $name
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest, $true)
        }
    } finally {
        $archive.Dispose()
    }
    return (Get-ChildItem $destDir -File).Count
}

function Write-AnimManifest($skinRoot, $skinId, $clips, $render = $null) {
    $manifest = @{
        schemaVersion = 1
        skinId = $skinId
        normalized = $false
        clips = $clips
    }
    if ($render) { $manifest.render = $render }
    $json = $manifest | ConvertTo-Json -Depth 6
    $path = Join-Path $skinRoot "anim_manifest.json"
    [System.IO.File]::WriteAllText($path, $json, [System.Text.UTF8Encoding]::new($false))
}

function Import-WalkDir($sourceDir, $skinRoot, $render = $null) {
    $walkDir = Join-Path $skinRoot "walk"
    if (Test-Path $walkDir) { Remove-Item $walkDir -Recurse -Force }
    Ensure-Dir $walkDir
    Copy-Item (Join-Path $sourceDir "*.png") $walkDir -Force
    $count = (Get-ChildItem $walkDir -File).Count
    $skinId = Split-Path $skinRoot -Leaf
    Write-AnimManifest $skinRoot $skinId @{ walk = $count } $render
    $walk1 = Join-Path $walkDir "walk_1.png"
    $previewSrc = Join-Path (Split-Path $sourceDir -Parent) "preview.png"
    if (Test-Path $previewSrc) {
        Copy-Item $previewSrc (Join-Path $skinRoot "preview.png") -Force
    } elseif (Test-Path $walk1) {
        Copy-Item $walk1 (Join-Path $skinRoot "preview.png") -Force
    }
    return $count
}

function Import-YishiWalkSkins($SkinsRoot, $YishiRoot, $LongWalkPipelineRoot) {
    $YishiWalkSkins = @(
        @{ Folder = "fire_long_walk";  Zip = "fire-long.zip";   RenamePrefix = "walk" },
        @{ Folder = "green_long_walk"; Zip = "green_long.zip";  RenamePrefix = "" },
        @{ Folder = "haimian_walk";    Zip = "haimian.zip";     RenamePrefix = "" },
        @{ Folder = "ice_long_walk";   Zip = "ice-long.zip";    RenamePrefix = "walk" },
        @{ Folder = "long_walk";       Zip = "long.zip";        RenamePrefix = ""; PipelineWalk = (Join-Path $LongWalkPipelineRoot "walk") },
        @{ Folder = "magic_dog_walk";  Zip = "magic_dog.zip";   RenamePrefix = "walk" },
        @{ Folder = "paidaxin_walk";   Zip = "paidaxin.zip";    RenamePrefix = "" },
        @{ Folder = "qishi_dog_walk";  Zip = "qishi_dog.zip";   RenamePrefix = "" },
        @{ Folder = "bl_long_walk";     Zip = "bl_long.zip";     RenamePrefix = "walk" }
    )

    Write-Host "`n[Yishi walk skins]" -ForegroundColor Yellow
    foreach ($skin in $YishiWalkSkins) {
        $skinRoot = Join-Path $SkinsRoot $skin.Folder
        Ensure-Dir $skinRoot
        if ($skin.PipelineWalk -and (Test-Path $skin.PipelineWalk)) {
            $render = @{ syncWalkCycleToSprite = $true }
            if ($skin.Folder -eq "long_walk") { $render.invertBitmapFacing = $true }
            $count = Import-WalkDir $skin.PipelineWalk $skinRoot $render
            Write-Host "  $($skin.Folder) : $count frames (pipeline)"
            Write-Host "  preview: $($skin.Folder)"
            continue
        }
        $walkDir = Join-Path $skinRoot "walk"
        $zipPath = Join-Path $YishiRoot $skin.Zip
        $count = Extract-ZipToDir $zipPath $walkDir $skin.RenamePrefix
        Write-Host "  $($skin.Folder) : $count frames"
        $render = $null
        if ($skin.Folder -eq "long_walk") {
            $render = @{ invertBitmapFacing = $true; syncWalkCycleToSprite = $true }
        } elseif ($skin.Folder -match "_walk$") {
            $render = @{ syncWalkCycleToSprite = $true }
        }
        Write-AnimManifest $skinRoot $skin.Folder @{ walk = $count } $render
        $walk1 = Join-Path $walkDir "walk_1.png"
        if (Test-Path $walk1) {
            Copy-Item $walk1 (Join-Path $skinRoot "preview.png") -Force
            Write-Host "  preview: $($skin.Folder)"
        }
    }
}

Write-Host "`n=== 豆人迷宫云端皮肤包构建 ===" -ForegroundColor Cyan
Write-Host "源: $DownloadRoot"
Write-Host "一十: $YishiRoot"
Write-Host "输出: $SkinsRoot"

if ($YishiOnly) {
    Ensure-Dir $SkinsRoot
    Import-YishiWalkSkins $SkinsRoot $YishiRoot $LongWalkPipelineRoot
    Write-Host "`n[Normalize + validate]" -ForegroundColor Yellow
    $NormalizeScript = Join-Path $PSScriptRoot "normalize_pac_maze_skin.py"
    $ValidateScript = Join-Path $PSScriptRoot "validate_pac_maze_skins.py"
    if (Get-Command python -ErrorAction SilentlyContinue) {
        python $NormalizeScript --all $SkinsRoot
        if ($LASTEXITCODE -ne 0) { throw "normalize_pac_maze_skin failed" }
        $PackScript = Join-Path $PSScriptRoot "pack_sprite_sheets.py"
        python $PackScript --all $SkinsRoot
        if ($LASTEXITCODE -ne 0) { throw "pack_sprite_sheets failed" }
        python $ValidateScript $SkinsRoot
        if ($LASTEXITCODE -ne 0) { throw "validate_pac_maze_skins failed" }
    }
    $allFiles = Get-ChildItem $SkinsRoot -Recurse -File
    $totalMb = [math]::Round(($allFiles | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
    Set-Content -Path (Join-Path $SkinsRoot "bundle_version.txt") -Value $BUNDLE_VERSION -Encoding ASCII -NoNewline
    Write-Host "`n完成(一十增量): $($allFiles.Count) 个文件, 合计 $totalMb MB (bundle_version=$BUNDLE_VERSION)" -ForegroundColor Green
    Write-Host "下一步: powershell -File backend/tools/upload_pac_maze_skins.ps1"
    exit 0
}

if (Test-Path $SkinsRoot) { Remove-Item $SkinsRoot -Recurse -Force }
Ensure-Dir $SkinsRoot

# —— 行走小鸡 Pro Max（多片段）——
$ProMaxRoot = Join-Path $SkinsRoot "food_chick_walker_pro_max"
$ProMaxClips = @("idle", "walk", "run", "jump", "attack", "die")
Write-Host "`n[Pro Max]" -ForegroundColor Yellow
$proMaxManifest = @{
    schemaVersion = 1
    skinId = "food_chick_walker_pro_max"
    normalized = $false
    clips = @{}
    render = @{
        syncWalkCycleToSprite = $true
        sampleSize = 1
    }
}
foreach ($clip in $ProMaxClips) {
    $zipPath = Join-Path $DownloadRoot "$clip.zip"
    $clipDir = Join-Path $ProMaxRoot $clip
    $count = Extract-ZipToDir $zipPath $clipDir
    if ($count -gt 0) {
        Write-Host "  $clip : $count frames"
        $proMaxManifest.clips[$clip] = $count
    }
}
Write-AnimManifest $ProMaxRoot "food_chick_walker_pro_max" $proMaxManifest.clips $proMaxManifest.render

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
    $render = $null
    if ($skin.Folder -eq "laoshu_walk" -or $skin.Folder -eq "wenzi_walk") {
        $render = @{ invertBitmapFacing = $true; syncWalkCycleToSprite = $true }
    } elseif ($skin.Folder -match "_walk$") {
        $render = @{ syncWalkCycleToSprite = $true }
    }
    Write-AnimManifest $skinRoot $skin.Folder @{ walk = $count } $render
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

Import-YishiWalkSkins $SkinsRoot $YishiRoot $LongWalkPipelineRoot

Write-Host "`n[Normalize + validate]" -ForegroundColor Yellow
$NormalizeScript = Join-Path $PSScriptRoot "normalize_pac_maze_skin.py"
$ValidateScript = Join-Path $PSScriptRoot "validate_pac_maze_skins.py"
if (Get-Command python -ErrorAction SilentlyContinue) {
    python $NormalizeScript --all $SkinsRoot
    if ($LASTEXITCODE -ne 0) { throw "normalize_pac_maze_skin failed" }
    $PackScript = Join-Path $PSScriptRoot "pack_sprite_sheets.py"
    python $PackScript --all $SkinsRoot
    if ($LASTEXITCODE -ne 0) { throw "pack_sprite_sheets failed" }
    python $ValidateScript $SkinsRoot
    if ($LASTEXITCODE -ne 0) { throw "validate_pac_maze_skins failed" }
} else {
    Write-Host "  python not found — skip normalize/validate (install Python + Pillow)" -ForegroundColor Yellow
}

$allFiles = Get-ChildItem $SkinsRoot -Recurse -File
$totalMb = [math]::Round(($allFiles | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
Set-Content -Path (Join-Path $SkinsRoot "bundle_version.txt") -Value $BUNDLE_VERSION -Encoding ASCII -NoNewline
Write-Host "`n完成: $($allFiles.Count) 个文件, 合计 $totalMb MB (bundle_version=$BUNDLE_VERSION)" -ForegroundColor Green
Write-Host "下一步: powershell -File backend/tools/upload_pac_maze_skins.ps1"
