# 从 dist/pac_maze_skins 发布：拼 sheet（幂等）→ 校验 → 写 bundle_version → 打 zip
# 用法:
#   powershell -File backend/tools/publish_pac_maze_skins.ps1
#   powershell -File backend/tools/publish_pac_maze_skins.ps1 -BundleVersion 23 -PruneFrames

param(
    [int]$BundleVersion = 23,
    [switch]$PruneFrames,
    [string]$SkinsRoot = "",
    [string]$OutZip = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $SkinsRoot) { $SkinsRoot = Join-Path $RepoRoot "dist\pac_maze_skins" }
if (-not $OutZip) { $OutZip = Join-Path $RepoRoot "dist\asset-bundles\pac_maze_skins.zip" }
$OutDir = Split-Path $OutZip -Parent
$PackScript = Join-Path $PSScriptRoot "pack_sprite_sheets.py"
$ValidateScript = Join-Path $PSScriptRoot "validate_pac_maze_skins.py"

if (-not (Test-Path $SkinsRoot)) {
    Write-Error "Missing skins root: $SkinsRoot — run build_pac_maze_skins.ps1 first."
}

Write-Host "`n=== Pack sprite sheets (all skins) ===" -ForegroundColor Cyan
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
if (Test-Path $OutZip) { Remove-Item $OutZip -Force }
$stagingParent = Join-Path $RepoRoot "dist\_zip_staging"
$stagingBundle = Join-Path $stagingParent "pac_maze_skins"
if (Test-Path $stagingParent) { Remove-Item $stagingParent -Recurse -Force }
New-Item -ItemType Directory -Force -Path $stagingBundle | Out-Null
Copy-Item "$SkinsRoot\*" $stagingBundle -Recurse -Force
Compress-Archive -Path $stagingBundle -DestinationPath $OutZip -CompressionLevel Optimal
Remove-Item $stagingParent -Recurse -Force -ErrorAction SilentlyContinue

$files = Get-ChildItem $SkinsRoot -Recurse -File
$mb = [math]::Round((Get-Item $OutZip).Length / 1MB, 2)
$srcMb = [math]::Round(($files | Measure-Object -Property Length -Sum).Sum / 1MB, 2)
$sheets = ($files | Where-Object { $_.Name -like '*_sheet.webp' }).Count
Write-Host "`n完成: $($files.Count) files ($sheets sheets), dist $srcMb MB -> zip $mb MB (bundle_version=$BundleVersion)" -ForegroundColor Green
Write-Host "下一步:"
Write-Host "  1. powershell -File pocketbase\tools\upload-assets-static.ps1 -Bundle pac_maze_skins -ZipPath $OutZip"
Write-Host "     （manifest 会自动写入 bundleVersion=$BundleVersion 与 CDN 缓存穿透 URL）"
Write-Host "  2. 可选：同步 ResourceStore.PAC_MAZE_SKINS_BUNDLE_VERSION 作无 manifest 时的兜底"
