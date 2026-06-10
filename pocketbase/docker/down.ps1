#Requires -Version 5.1
param([switch]$Volumes)

$pbDir = Split-Path $PSScriptRoot -Parent
Set-Location $pbDir

$args = @("compose", "--profile", "push", "--profile", "tunnel", "down")
if ($Volumes) { $args += "-v" }

Write-Host "docker $($args -join ' ')" -ForegroundColor Cyan
docker @args
