# 打包本地 assets 并上传到 CloudBase 云存储 yishi-assetss/v1/bundles
# 用法: powershell -File backend/tools/upload_asset_bundles.ps1
# 前置: tcb login 且 backend/cloudbaserc.json envId 正确

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$AssetsRoot = Join-Path $RepoRoot "app\src\main\assets"
$OutDir = Join-Path $RepoRoot "dist\asset-bundles"
$CloudPrefix = "yishi-assetss/v1/bundles"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Ensure-Zip($folderName, $existingZip = $null) {
    $zipPath = Join-Path $OutDir "$folderName.zip"
    if ($existingZip -and (Test-Path $existingZip)) {
        Copy-Item $existingZip $zipPath -Force
        Write-Host "  reuse $folderName.zip from assets root"
        return $zipPath
    }
    $src = Join-Path $AssetsRoot $folderName
    if (-not (Test-Path $src)) {
        Write-Host "  skip $folderName (missing)"
        return $null
    }
    Write-Host "  zipping $folderName ..."
    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
    Compress-Archive -Path $src -DestinationPath $zipPath -CompressionLevel Optimal
    $mb = [math]::Round((Get-Item $zipPath).Length / 1MB, 1)
    Write-Host "  -> $folderName.zip ($mb MB)"
    return $zipPath
}

Write-Host "`n=== [1/2] 打包 ===" -ForegroundColor Cyan
$bundles = @(
    @{ Name = "dibu"; Existing = $null },
    @{ Name = "wheel"; Existing = (Join-Path $AssetsRoot "wheel.zip") },
    @{ Name = "renge"; Existing = $null },
    @{ Name = "login"; Existing = $null },
    @{ Name = "pet"; Existing = $null },
    @{ Name = "xiangkuang"; Existing = (Join-Path $AssetsRoot "xiangkuang.zip") }
)

$zipFiles = @()
foreach ($b in $bundles) {
    $z = Ensure-Zip $b.Name $b.Existing
    if ($z) { $zipFiles += $z }
}

Write-Host "`n=== [2/2] 上传到 CloudBase: $CloudPrefix ===" -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot "backend")
$i = 0
foreach ($zip in $zipFiles) {
    $i++
    $name = [IO.Path]::GetFileName($zip)
    $cloudPath = "$CloudPrefix/$name"
    Write-Host "[$i/$($zipFiles.Count)] upload $name -> $cloudPath"
    tcb storage upload $zip $cloudPath --times 3
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        Write-Host "上传失败: $name" -ForegroundColor Red
        exit 1
    }
}

$manifestLocal = Join-Path $PSScriptRoot "asset_manifest.json"
$manifestCloud = "yishi-assetss/v1/manifest.json"
Write-Host "`n上传 manifest.json -> $manifestCloud"
tcb storage upload $manifestLocal $manifestCloud --times 3
if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }

Pop-Location
Write-Host "`n全部上传完成。控制台路径: 云存储 / yishi-assetss / v1 / bundles" -ForegroundColor Green
Write-Host "manifest: https://6675-funlife-prod-d8gxf7og0518b8253-1333176506.tcb.qcloud.la/yishi-assetss/v1/manifest.json" -ForegroundColor Gray
