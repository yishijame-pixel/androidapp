# 部署 asset_bundle 云函数 + HTTP 路由
# 用法: .\deploy-asset-bundle.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "`n=== 1/2 部署 asset_bundle ===" -ForegroundColor Cyan
Push-Location functions/asset_bundle
if (-not (Test-Path node_modules)) { npm install --production }
Pop-Location
tcb fn deploy asset_bundle --force
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n=== 2/2 HTTP 路由 /asset_bundle ===" -ForegroundColor Cyan
$existing = tcb service list 2>&1 | Out-String
if ($existing -match "/asset_bundle") {
    Write-Host "HTTP 路由已存在" -ForegroundColor Yellow
} else {
    tcb service create -p asset_bundle -f asset_bundle
}

Write-Host "`n完成。测试: node tools/test_asset_bundle.js" -ForegroundColor Green
