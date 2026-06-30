# 从已有 pac_maze_skins.zip 解压 → 拼 sprite sheet → 校验 → 重新打 zip
# 用法: powershell -File backend/tools/repack_pac_maze_sheets.ps1 [-PruneFrames] [-BundleVersion 22]

param(
    [switch]$PruneFrames,
    [int]$BundleVersion = 23,
    [string]$ZipPath = "",
    [string]$SkinsRoot = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $SkinsRoot) { $SkinsRoot = Join-Path $RepoRoot "dist\pac_maze_skins" }
if (-not $ZipPath) { $ZipPath = Join-Path $RepoRoot "dist\asset-bundles\pac_maze_skins.zip" }
$OutDir = Join-Path $RepoRoot "dist\asset-bundles"
$PackScript = Join-Path $PSScriptRoot "pack_sprite_sheets.py"
$ValidateScript = Join-Path $PSScriptRoot "validate_pac_maze_skins.py"

if (-not (Test-Path $ZipPath)) {
    Write-Error "Missing zip: $ZipPath — run build_pac_maze_skins.ps1 or upload first."
}

Write-Host "`n=== Extract $ZipPath ===" -ForegroundColor Cyan
if (Test-Path $SkinsRoot) { Remove-Item $SkinsRoot -Recurse -Force }
New-Item -ItemType Directory -Force -Path $SkinsRoot | Out-Null
Expand-Archive -Path $ZipPath -DestinationPath (Split-Path $SkinsRoot -Parent) -Force

Write-Host "`n=== Pack sprite sheets ===" -ForegroundColor Cyan
$packArgs = @($PackScript, "--all", $SkinsRoot)
if ($PruneFrames) { $packArgs = @($PackScript, "--prune-frames", "--all", $SkinsRoot) }
python @packArgs
if ($LASTEXITCODE -ne 0) { throw "pack_sprite_sheets failed" }

Write-Host "`n=== Validate ===" -ForegroundColor Cyan
python $ValidateScript $SkinsRoot
if ($LASTEXITCODE -ne 0) { throw "validate_pac_maze_skins failed" }

Set-Content -Path (Join-Path $SkinsRoot "bundle_version.txt") -Value $BundleVersion -Encoding ASCII -NoNewline

Write-Host "`n=== Zip pac_maze_skins (v$BundleVersion) ===" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$NewZip = Join-Path $OutDir "pac_maze_skins.zip"
if (Test-Path $NewZip) { Remove-Item $NewZip -Force }
$stagingParent = Join-Path $RepoRoot "dist\_zip_staging"
$stagingBundle = Join-Path $stagingParent "pac_maze_skins"
if (Test-Path $stagingParent) { Remove-Item $stagingParent -Recurse -Force }
New-Item -ItemType Directory -Force -Path $stagingBundle | Out-Null
Copy-Item "$SkinsRoot\*" $stagingBundle -Recurse -Force
Compress-Archive -Path $stagingBundle -DestinationPath $NewZip -CompressionLevel Optimal
Remove-Item $stagingParent -Recurse -Force -ErrorAction SilentlyContinue

$files = Get-ChildItem $SkinsRoot -Recurse -File
$mb = [math]::Round((Get-Item $NewZip).Length / 1MB, 2)
$srcMb = [math]::Round(($files | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
Write-Host "`n完成: $($files.Count) files, dist $srcMb MB -> zip $mb MB (bundle_version=$BundleVersion)" -ForegroundColor Green
Write-Host "下一步:"
Write-Host "  1. powershell -File backend/tools/copy_platformer_chick_bootstrap.ps1"
Write-Host "  2. powershell -File backend/tools/upload_pac_maze_skins.ps1  (可选上传 CDN)"
