# 部署聊天 AI 卡密相关云函数（redeem + chat_ai）
# 用法: .\deploy-chat-ai-card.ps1
# 前置: npm i -g @cloudbase/cli  且已 tcb login

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Set-Location $PSScriptRoot

Write-Host "`n=== 1/2 同步 shared/sku.js 到各云函数 ===" -ForegroundColor Cyan
node sync-sku.js
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n=== 2/2 部署 redeem、chat_ai ===" -ForegroundColor Cyan
tcb fn deploy redeem --force
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

tcb fn deploy chat_ai --force
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n✅ 部署完成。运行 E2E 测试:" -ForegroundColor Green
Write-Host "   cd tools" -ForegroundColor Gray
Write-Host "   node test_chat_ai_card_e2e.js --no-llm" -ForegroundColor Gray
Write-Host "   node test_chat_ai_card_e2e.js" -ForegroundColor Gray
