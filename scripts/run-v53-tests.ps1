# run-v53-tests.ps1 — v53 阅光书房 · 端到端单元测试运行器
#
# 用法（PowerShell）：
#   .\scripts\run-v53-tests.ps1            # 跑全部 v53 测试
#   .\scripts\run-v53-tests.ps1 -OnlyVip   # 只跑 VipQuota
#   .\scripts\run-v53-tests.ps1 -Report    # 跑完后打开 HTML 报告

[CmdletBinding()]
param(
    [switch]$OnlyVip,
    [switch]$Report,
    [switch]$Detailed
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."
Set-Location $root

Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  v53 阅光书房 · 单元测试运行器" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

# 选择测试范围
$filter = if ($OnlyVip) {
    "*VipQuotaV53Test"
} else {
    "*V53*,*BookChatGate*"
}

Write-Host "`n[1/3] 编译验证..." -ForegroundColor Yellow
& .\gradlew.bat compileDebugKotlin --no-daemon -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 编译失败" -ForegroundColor Red
    exit 1
}
Write-Host "✓ 编译通过" -ForegroundColor Green

Write-Host "`n[2/3] 跑单元测试 (filter: $filter) ..." -ForegroundColor Yellow
$logArgs = if ($Detailed) { "--info" } else { "--no-daemon" }

if ($OnlyVip) {
    & .\gradlew.bat testDebugUnitTest --tests "com.example.funlife.vip.VipQuotaV53Test" $logArgs
} else {
    & .\gradlew.bat testDebugUnitTest `
        --tests "com.example.funlife.vip.VipQuotaV53Test" `
        --tests "com.example.funlife.data.V53DaoIsolationTest" `
        --tests "com.example.funlife.repository.QuoteRepositoryV53Test" `
        --tests "com.example.funlife.repository.ReadingRepositoryV53Test" `
        --tests "com.example.funlife.viewmodel.BookChatGateLogicTest" `
        $logArgs
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ 有测试失败，查看 app\build\reports\tests\testDebugUnitTest\index.html" -ForegroundColor Red
    if ($Report) {
        Start-Process "$root\app\build\reports\tests\testDebugUnitTest\index.html"
    }
    exit 1
}

Write-Host "`n[3/3] 汇总结果..." -ForegroundColor Yellow
$xmls = Get-ChildItem "$root\app\build\test-results\testDebugUnitTest\TEST-*V53*.xml","$root\app\build\test-results\testDebugUnitTest\TEST-*BookChatGate*.xml" -ErrorAction SilentlyContinue
$totalTests = 0
$totalFailures = 0
$totalErrors = 0
foreach ($xml in $xmls) {
    [xml]$doc = Get-Content $xml.FullName
    $suite = $doc.testsuite
    $totalTests += [int]$suite.tests
    $totalFailures += [int]$suite.failures
    $totalErrors += [int]$suite.errors
    $color = if ([int]$suite.failures -eq 0 -and [int]$suite.errors -eq 0) { "Green" } else { "Red" }
    Write-Host ("  {0,-65} {1,4} tests / {2,2} fail / {3,2} err" -f $suite.name, $suite.tests, $suite.failures, $suite.errors) -ForegroundColor $color
}

Write-Host ""
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
$summary = "  汇总: $totalTests 用例 / $totalFailures 失败 / $totalErrors 错误"
$col = if ($totalFailures -eq 0 -and $totalErrors -eq 0) { "Green" } else { "Red" }
Write-Host $summary -ForegroundColor $col
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

if ($Report) {
    Write-Host "`n打开 HTML 报告..." -ForegroundColor Yellow
    $reportPath = Join-Path $root "app\build\reports\tests\testDebugUnitTest\index.html"
    Start-Process $reportPath
}

if ($totalFailures -gt 0 -or $totalErrors -gt 0) { exit 1 }
exit 0
