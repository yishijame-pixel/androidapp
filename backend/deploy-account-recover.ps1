# 部署账号恢复云函数 account_recover
# 用法: .\deploy-account-recover.ps1
# 前置: npm i -g @cloudbase/cli  且已 tcb login
# 部署后: 控制台 → account_recover → HTTP 触发器 → 路径 /account_recover

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Set-Location $PSScriptRoot

Write-Host "`n=== 1/3 同步 shared 到 account_recover ===" -ForegroundColor Cyan
node sync-sku.js
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n=== 2/3 安装依赖 ===" -ForegroundColor Cyan
Push-Location functions/account_recover
if (-not (Test-Path node_modules)) {
    npm install --production
    if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
}
Pop-Location

Write-Host "`n=== 3/4 部署 account_recover ===" -ForegroundColor Cyan
tcb fn deploy account_recover --force
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n=== 4/4 配置 HTTP 访问 /account_recover ===" -ForegroundColor Cyan
$existing = tcb service list 2>&1 | Out-String
if ($existing -match "/account_recover") {
    Write-Host "HTTP 路由已存在，跳过创建" -ForegroundColor Yellow
} else {
    tcb service create -p account_recover -f account_recover
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "`n部署完成。HTTP 端点:" -ForegroundColor Green
Write-Host "   https://funlife-prod-d8gxf7og0518b8253-1333176506.ap-shanghai.app.tcloudbase.com/account_recover" -ForegroundColor Gray
Write-Host "  cd tools" -ForegroundColor Gray
Write-Host "  node test_account_recover.js" -ForegroundColor Gray
Write-Host "或:" -ForegroundColor Gray
Write-Host "  powershell -File tools/run_account_recovery_tests.ps1 -IncludeE2E" -ForegroundColor Gray
