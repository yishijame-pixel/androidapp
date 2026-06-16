# Pac-Maze 权威 WebSocket 服（开发）

$ErrorActionPreference = "Stop"
$env:PB_BASE_URL = if ($env:PB_BASE_URL) { $env:PB_BASE_URL } else { "http://127.0.0.1:8090" }
$env:PAC_MAZE_WS_PORT = if ($env:PAC_MAZE_WS_PORT) { $env:PAC_MAZE_WS_PORT } else { "8791" }

Write-Host "PB_BASE_URL=$env:PB_BASE_URL"
Write-Host "PAC_MAZE_WS_PORT=$env:PAC_MAZE_WS_PORT"

Set-Location (Join-Path $PSScriptRoot "..")
.\gradlew :pac-maze-server:run --no-daemon
