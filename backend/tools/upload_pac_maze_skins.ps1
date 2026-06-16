# Pack dist/pac_maze_skins and upload to CloudBase COS
# Run build_pac_maze_skins.ps1 first; requires tcb login

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$SrcDir = Join-Path $RepoRoot "dist\pac_maze_skins"
$OutDir = Join-Path $RepoRoot "dist\asset-bundles"
$ZipPath = Join-Path $OutDir "pac_maze_skins.zip"
$CloudPrefix = "yishi-assetss/v1/bundles"

if (-not (Test-Path $SrcDir)) {
    Write-Host "Missing $SrcDir - run build_pac_maze_skins.ps1 first" -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "`n=== Zip pac_maze_skins ===" -ForegroundColor Cyan
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
$stagingParent = Join-Path $RepoRoot "dist\_zip_staging"
$stagingBundle = Join-Path $stagingParent "pac_maze_skins"
if (Test-Path $stagingParent) { Remove-Item $stagingParent -Recurse -Force }
New-Item -ItemType Directory -Force -Path $stagingBundle | Out-Null
Copy-Item "$SrcDir\*" $stagingBundle -Recurse -Force
Compress-Archive -Path $stagingBundle -DestinationPath $ZipPath -CompressionLevel Optimal
Remove-Item $stagingParent -Recurse -Force -ErrorAction SilentlyContinue

$mb = [math]::Round((Get-Item $ZipPath).Length / 1MB, 2)
Write-Host "-> pac_maze_skins.zip ($mb MB)"

Write-Host "`n=== Upload to $CloudPrefix ===" -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot "backend")
$cloudPath = "$CloudPrefix/pac_maze_skins.zip"
Write-Host "upload pac_maze_skins.zip -> $cloudPath"
tcb storage upload $ZipPath $cloudPath --times 3
$code = $LASTEXITCODE
Pop-Location

if ($code -ne 0) {
    Write-Host "Upload failed (run tcb login first)" -ForegroundColor Red
    exit 1
}

Write-Host "`nUpload OK." -ForegroundColor Green
Write-Host "COS: yishi-assetss/v1/bundles/pac_maze_skins.zip"
Write-Host "Update asset_manifest.json + redeploy asset_bundle cloud function"
