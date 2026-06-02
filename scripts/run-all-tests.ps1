#!/usr/bin/env pwsh
# run-all-tests.ps1 — v53/v54 全栈测试一键脚本（CI 友好）
#
# 跑顺序：
#   1. 后端 Node.js 语法检查（4 个云函数 + admin server.js + test_admin.js）
#   2. 后端敏感词过滤器单元测试（36 用例）
#   3. Kotlin 编译检查（gradlew compileDebugKotlin -q）
#   4. Kotlin 单元测试（gradlew testDebugUnitTest）
#   5. 汇总并退出码：任一阶段失败则 exit 1
#
# 用法：
#   .\scripts\run-all-tests.ps1                  # 跑全部
#   .\scripts\run-all-tests.ps1 -SkipNode        # 跳过 Node 测试
#   .\scripts\run-all-tests.ps1 -SkipKotlin      # 跳过 Kotlin 测试
#   .\scripts\run-all-tests.ps1 -CI              # CI 模式（无颜色，纯日志）

[CmdletBinding()]
param(
    [switch]$SkipNode,
    [switch]$SkipKotlin,
    [switch]$CI
)

$ErrorActionPreference = "Continue"   # 单阶段失败不中断后续阶段，最后统一报告
$root = Resolve-Path "$PSScriptRoot\.."
Set-Location $root

# 控制台颜色（CI 模式关闭）
function Header($msg) {
    if ($CI) { Write-Host "==== $msg ====" }
    else {
        Write-Host ""
        Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  $msg" -ForegroundColor Cyan
        Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
    }
}
function Ok($msg) { if ($CI) { Write-Host "[PASS] $msg" } else { Write-Host "  ✓ $msg" -ForegroundColor Green } }
function Bad($msg) { if ($CI) { Write-Host "[FAIL] $msg" } else { Write-Host "  ✗ $msg" -ForegroundColor Red } }
function Info($msg) { if ($CI) { Write-Host "  $msg" } else { Write-Host "  $msg" -ForegroundColor Gray } }

$results = @()
function Track($stage, $exitCode, $detail = "") {
    $script:results += [PSCustomObject]@{
        Stage = $stage
        Pass = ($exitCode -eq 0)
        Detail = $detail
    }
    if ($exitCode -eq 0) { Ok "$stage" } else { Bad "$stage  $detail" }
}

$startTime = Get-Date

# ════════════════════════════════════════════════════════════
# Phase 1: 后端 Node.js
# ════════════════════════════════════════════════════════════
if (-not $SkipNode) {
    Header "Phase 1: 后端 Node.js"

    $nodeFiles = @(
        "backend\admin\server.js",
        "backend\admin\test_admin.js",
        "backend\functions\chat_ai\index.js",
        "backend\functions\quote_galaxy\index.js",
        "backend\functions\quote_galaxy\sensitive-filter.js",
        "backend\functions\postcard_drift\index.js",
        "backend\functions\postcard_drift\sensitive-filter.js"
    )
    $syntaxFails = 0
    foreach ($f in $nodeFiles) {
        $full = Join-Path $root $f
        if (-not (Test-Path $full)) { Bad "missing: $f"; $syntaxFails++; continue }
        & node --check $full 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { Bad "syntax error: $f"; $syntaxFails++ }
    }
    Track "Node 语法检查（$($nodeFiles.Count) 文件）" $syntaxFails

    $filterTest = Join-Path $root "backend\functions\quote_galaxy\sensitive-filter.test.js"
    & node $filterTest 2>&1 | Out-Null
    Track "敏感词过滤器测试（36 用例）" $LASTEXITCODE
}

# ════════════════════════════════════════════════════════════
# Phase 2: Kotlin 客户端
# ════════════════════════════════════════════════════════════
if (-not $SkipKotlin) {
    Header "Phase 2: Kotlin 客户端"

    $gradleArgs = if ($CI) { "--no-daemon -q --console=plain" } else { "--no-daemon -q" }

    Info "编译验证..."
    Invoke-Expression ".\gradlew.bat compileDebugKotlin $gradleArgs" 2>&1 | Out-Null
    Track "Kotlin 编译（compileDebugKotlin）" $LASTEXITCODE

    Info "单元测试..."
    Invoke-Expression ".\gradlew.bat testDebugUnitTest $gradleArgs" 2>&1 | Out-Null
    $testExit = $LASTEXITCODE
    Track "Kotlin 单元测试（testDebugUnitTest）" $testExit

    # 汇总各 suite
    if (Test-Path "$root\app\build\test-results\testDebugUnitTest") {
        $totalT = 0; $totalF = 0; $totalE = 0
        $xmls = Get-ChildItem "$root\app\build\test-results\testDebugUnitTest\*.xml" -ErrorAction SilentlyContinue
        foreach ($xml in $xmls) {
            try {
                [xml]$doc = Get-Content $xml.FullName
                $s = $doc.testsuite
                $t = [int]$s.tests; $f = [int]$s.failures; $e = [int]$s.errors
                $totalT += $t; $totalF += $f; $totalE += $e
                $name = $s.name -replace 'com\.example\.funlife\.', ''
                if ($f -eq 0 -and $e -eq 0) {
                    Info ("  · $name : $t tests")
                } else {
                    Bad ("  · $name : $t tests / $f fail / $e err")
                }
            } catch {}
        }
        Info ""
        Info "Kotlin 测试合计：$totalT 用例 / $totalF 失败 / $totalE 错误"
    }
}

# ════════════════════════════════════════════════════════════
# Phase 3: 汇总
# ════════════════════════════════════════════════════════════
$elapsed = ((Get-Date) - $startTime).TotalSeconds
$failed = ($results | Where-Object { -not $_.Pass }).Count
$total = $results.Count

Header "汇总"
foreach ($r in $results) {
    if ($r.Pass) { Ok $r.Stage } else { Bad "$($r.Stage)  $($r.Detail)" }
}
if (-not $CI) {
    Write-Host ""
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
    if ($failed -eq 0) {
        Write-Host "  ★ 全部 $total 阶段通过 · 耗时 $([math]::Round($elapsed, 1)) 秒" -ForegroundColor Green
    } else {
        Write-Host "  $failed / $total 阶段失败 · 耗时 $([math]::Round($elapsed, 1)) 秒" -ForegroundColor Red
    }
    Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "RESULT: $($total - $failed)/$total stages passed in ${elapsed}s"
}

if ($failed -gt 0) { exit 1 }
exit 0
