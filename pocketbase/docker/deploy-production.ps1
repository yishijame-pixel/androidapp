#Requires -Version 5.1
# FunLife 生产 Docker 一键部署（PocketBase + draw_ws + pac-maze-ws + FCM + Cloudflare 隧道）
#
# 用法:
#   cd d:\soft\pocketbase
#   .\docker\deploy-production.ps1
#
# 仅重建 pac-maze-ws:
#   .\docker\deploy-production.ps1 -PacMazeOnly

param(
    [switch]$PacMazeOnly,
    [switch]$SkipBuild,
    [switch]$SkipTunnel,
    [string]$PublicHost = "https://pb.yishi.site"
)

$ErrorActionPreference = "Stop"
$pbDir = Split-Path $PSScriptRoot -Parent
$repoRoot = Split-Path $pbDir -Parent
Set-Location $pbDir

function Write-Step($msg) { Write-Host "`n>> $msg" -ForegroundColor Cyan }

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "请先安装 Docker Desktop" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path ".env")) {
    if (Test-Path "docker\.env.example") {
        Copy-Item "docker\.env.example" ".env"
        Write-Host "已创建 .env" -ForegroundColor Yellow
    }
}

# 确保 Docker 版 cloudflared 配置存在（含 /pac-maze-ws 路由）
$dockerCfDir = Join-Path $PSScriptRoot "cloudflared"
$dockerCfConfig = Join-Path $dockerCfDir "config.yml"
$userCfConfig = Join-Path $env:USERPROFILE ".cloudflared\config.yml"

if (-not $SkipTunnel -and -not (Test-Path $dockerCfConfig)) {
    Write-Step "初始化 docker/cloudflared/config.yml"
    if (-not (Test-Path $userCfConfig)) {
        Write-Host "缺少 $userCfConfig，请先运行 .\setup-tunnel-yishi.ps1" -ForegroundColor Red
        exit 1
    }
    $uuid = (Get-Content $userCfConfig | Where-Object { $_ -match '^\s*tunnel:\s*' } | Select-Object -First 1) -replace '^\s*tunnel:\s*', ''
    if (-not $uuid) {
        Write-Host "无法从 $userCfConfig 读取 tunnel UUID" -ForegroundColor Red
        exit 1
    }
    $credSrc = Join-Path $env:USERPROFILE ".cloudflared\$uuid.json"
    if (-not (Test-Path $credSrc)) {
        Write-Host "缺少隧道凭证: $credSrc" -ForegroundColor Red
        exit 1
    }
    New-Item -ItemType Directory -Force -Path $dockerCfDir | Out-Null
    Copy-Item $credSrc (Join-Path $dockerCfDir "$uuid.json") -Force
    Copy-Item (Join-Path $dockerCfDir "config.example.yml") $dockerCfConfig -Force
    (Get-Content $dockerCfConfig -Raw) -replace '<TUNNEL_UUID>', $uuid | Set-Content $dockerCfConfig -Encoding UTF8
    Write-Host "[OK] 已生成 $dockerCfConfig" -ForegroundColor Green
}

if (-not $SkipBuild) {
    Write-Step "Gradle :pac-maze-server:installDist"
    Set-Location $repoRoot
    & .\gradlew.bat :pac-maze-server:installDist --no-daemon -q
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Set-Location $pbDir
}

$profiles = @()
if (Test-Path "secrets\firebase-adminsdk.json") { $profiles += "push" }
if (-not $SkipTunnel -and (Test-Path $dockerCfConfig)) { $profiles += "tunnel" }

$composeArgs = @("compose")
foreach ($p in $profiles) { $composeArgs += @("--profile", $p) }

if (-not $SkipBuild) {
    if ($PacMazeOnly) {
        Write-Step "Docker build pac-maze-ws"
        $composeArgs += @("build", "pac-maze-ws")
    } else {
        Write-Step "Docker build (全部服务)"
        $composeArgs += "build"
    }
    & docker @composeArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $composeArgs = @("compose")
    foreach ($p in $profiles) { $composeArgs += @("--profile", $p) }
}

Write-Step "Docker up -d"
if ($PacMazeOnly) {
    $composeArgs += @("up", "-d", "pac-maze-ws")
} else {
    $composeArgs += @("up", "-d")
}
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Start-Sleep -Seconds 8

Write-Step "本地健康检查"
$localChecks = @(
    @{ Name = "PocketBase"; Url = "http://127.0.0.1:8090/api/health" },
    @{ Name = "draw_ws"; Url = "http://127.0.0.1:8790/health" },
    @{ Name = "pac-maze-ws"; Url = "http://127.0.0.1:8791/health" }
)
$localOk = $true
foreach ($c in $localChecks) {
    try {
        $r = Invoke-RestMethod $c.Url -TimeoutSec 15
        Write-Host "  [OK] $($c.Name) $($r | ConvertTo-Json -Compress)" -ForegroundColor Green
    } catch {
        Write-Host "  [FAIL] $($c.Name) $($_.Exception.Message)" -ForegroundColor Red
        $localOk = $false
    }
}

if ($profiles -contains "tunnel") {
    Write-Step "公网健康检查 ($PublicHost)"
    $publicChecks = @(
        "$PublicHost/api/health",
        "$PublicHost/draw-ws/health",
        "$PublicHost/pac-maze-ws/health"
    )
    foreach ($url in $publicChecks) {
        try {
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            $r = Invoke-RestMethod $url -TimeoutSec 20
            $sw.Stop()
            Write-Host "  [OK] $url ($($sw.ElapsedMilliseconds)ms) $($r | ConvertTo-Json -Compress)" -ForegroundColor Green
        } catch {
            Write-Host "  [FAIL] $url $($_.Exception.Message)" -ForegroundColor Red
            $localOk = $false
        }
    }
}

Write-Host ""
if ($localOk) {
    Write-Host "部署完成。Android 配置 POCKETBASE_URL=$PublicHost，PAC_MAZE_WS_URL 留空即可。" -ForegroundColor Green
} else {
    Write-Host "部分检查失败，查看日志: docker compose logs pac-maze-ws cloudflared" -ForegroundColor Yellow
    exit 1
}
