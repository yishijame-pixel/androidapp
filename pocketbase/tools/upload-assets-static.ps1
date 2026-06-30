#Requires -Version 5.1
# 发布游戏资源到 assets_public/ 并重新生成 manifest.json
#
# 用法:
#   .\tools\upload-assets-static.ps1
#   .\tools\upload-assets-static.ps1 -Bundle pac_maze_skins -ZipPath d:\soft\dist\asset-bundles\pac_maze_skins.zip
#   .\tools\upload-assets-static.ps1 -RestartServices

param(
    [string]$Bundle = "",
    [string]$ZipPath = "",
    [string]$BaseUrl = "https://assets.yishi.site",
    [switch]$RestartServices
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$pbDir = Split-Path $PSScriptRoot -Parent
$bundlesDir = Join-Path $pbDir "assets_public\bundles"
New-Item -ItemType Directory -Force -Path $bundlesDir | Out-Null

function Copy-BundleZip($name, $src) {
    if (-not (Test-Path $src)) {
        Write-Host "  skip $name (missing $src)" -ForegroundColor Yellow
        return $false
    }
    $dest = Join-Path $bundlesDir "$name.zip"
    Copy-Item $src $dest -Force
    $mb = [math]::Round((Get-Item $dest).Length / 1MB, 2)
    Write-Host "  -> $name.zip ($mb MB)" -ForegroundColor Green
    return $true
}

Write-Host "`n=== [1/3] 复制 zip 到 assets_public/bundles ===" -ForegroundColor Cyan

if ($Bundle -and $ZipPath) {
    Copy-BundleZip $Bundle $ZipPath | Out-Null
} elseif ($Bundle) {
    $repoRoot = Split-Path $pbDir -Parent
    $guess = Join-Path $repoRoot "dist\asset-bundles\$Bundle.zip"
    Copy-BundleZip $Bundle $guess | Out-Null
} else {
    $repoRoot = Split-Path $pbDir -Parent
    $dist = Join-Path $repoRoot "dist\asset-bundles"
    $map = @{
        pac_maze_skins        = Join-Path $dist "pac_maze_skins.zip"
        pac_maze_sfx          = Join-Path $dist "pac_maze_sfx.zip"
        platformer_characters = Join-Path $dist "platformer_characters.zip"
        platformer_sfx        = Join-Path $dist "platformer_sfx.zip"
        platformer_supertux   = Join-Path $dist "platformer_supertux.zip"
    }
    foreach ($k in $map.Keys) {
        Copy-BundleZip $k $map[$k] | Out-Null
    }
}

Write-Host "`n=== [2/3] 生成 manifest.json ===" -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "generate-assets-manifest.ps1") -BaseUrl $BaseUrl
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n=== [3/3] 验证 ===" -ForegroundColor Cyan
try {
    $local = Invoke-RestMethod "http://127.0.0.1:8098/manifest.json" -TimeoutSec 3
    Write-Host "  local :8098 manifest v$($local.version) OK" -ForegroundColor Green
} catch {
    Write-Host "  local :8098 未就绪（需 docker compose up assets-static）" -ForegroundColor DarkGray
}

try {
    $pub = Invoke-RestMethod "$BaseUrl/manifest.json" -TimeoutSec 8
    Write-Host "  public $BaseUrl manifest v$($pub.version) OK" -ForegroundColor Green
} catch {
    Write-Host "  public manifest 暂不可达: $($_.Exception.Message)" -ForegroundColor Yellow
}

if ($RestartServices) {
    Write-Host "`n=== 重启 Docker 服务 ===" -ForegroundColor Cyan
    Push-Location $pbDir
    docker compose up -d assets-static
    docker compose --profile tunnel restart cloudflared
    Pop-Location
}

Write-Host "`n完成。详见 docs/game-assets-static-hosting.md" -ForegroundColor Green
