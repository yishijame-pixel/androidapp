# 构建并推送 Docker 镜像（按需修改 REGISTRY）

param(
    [string]$Registry = "",
    [string]$Tag = "latest"
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $root

$image = if ($Registry) { "${Registry}/funlife-pac-maze-ws:${Tag}" } else { "funlife/pac-maze-ws:${Tag}" }

Write-Host "Building $image ..."
docker build -f pac-maze-server/Dockerfile -t $image .

Write-Host "Built $image"
Write-Host "Run: docker run -d -p 8791:8791 -e PB_BASE_URL=http://host.docker.internal:8090 $image"
