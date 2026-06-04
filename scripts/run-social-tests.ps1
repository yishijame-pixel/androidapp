#!/usr/bin/env pwsh
# run-social-tests.ps1 — PocketBase 社交企业级测试（E2E + Kotlin 纯逻辑）
#
# 用法：
#   .\scripts\run-social-tests.ps1
#   .\scripts\run-social-tests.ps1 -BaseUrl http://192.168.0.100:8090
#   .\scripts\run-social-tests.ps1 -SkipE2E          # 只跑 Kotlin 单元测试
#   .\scripts\run-social-tests.ps1 -SkipKotlin
#   .\scripts\run-social-tests.ps1 -SetupSchema      # E2E 前自动跑 setup-schema.ps1
#   .\scripts\run-social-tests.ps1 -KeepData         # 保留 E2E 测试账号
#   .\scripts\run-social-tests.ps1 -CI

[CmdletBinding()]
param(
    [string]$BaseUrl = "http://127.0.0.1:8090",
    [switch]$SkipE2E,
    [switch]$SkipKotlin,
    [switch]$SetupSchema,
    [switch]$KeepData,
    [switch]$CI
)

$ErrorActionPreference = "Continue"
$root = Resolve-Path "$PSScriptRoot\.."
Set-Location $root

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
function Info($msg) { Write-Host "  $msg" -ForegroundColor Gray }

$results = @()
function Track($stage, $exitCode, $detail = "") {
    $script:results += [PSCustomObject]@{ Stage = $stage; Pass = ($exitCode -eq 0); Detail = $detail }
    if ($exitCode -eq 0) { Ok $stage } else { Bad "$stage  $detail" }
}

$startTime = Get-Date

# ── Phase 0: PocketBase 可达性 ──
Header "Phase 0: PocketBase 健康检查"
$healthOk = $false
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/health" -Method GET -TimeoutSec 5
    $healthOk = $true
    Info "health: $($health | ConvertTo-Json -Compress)"
} catch {
    Bad "无法连接 $BaseUrl — 请先运行 pocketbase\start.ps1"
    Info $_.Exception.Message
}
Track "PocketBase 健康检查" $(if ($healthOk) { 0 } else { 1 })

if (-not $healthOk -and -not $SkipE2E) {
    Header "汇总"
    Bad "PocketBase 未启动，E2E 跳过。可 -SkipE2E 仅跑 Kotlin。"
    exit 1
}

# ── Phase 1: Schema（可选）──
if ($SetupSchema -and $healthOk) {
    Header "Phase 1: Schema 同步"
    $schemaScript = Join-Path $root "pocketbase\setup-schema.ps1"
    if (-not (Test-Path $schemaScript)) {
        Track "setup-schema.ps1 存在" 1
    } else {
        & $schemaScript -BaseUrl $BaseUrl 2>&1 | Out-Host
        Track "setup-schema.ps1" $LASTEXITCODE
    }
}

# ── Phase 2: Node E2E ──
if (-not $SkipE2E -and $healthOk) {
    Header "Phase 2: 社交 E2E（Node）"
    $e2eScript = Join-Path $root "pocketbase\tools\test_social_e2e.js"
    $nodeArgs = @($e2eScript, "--base-url", $BaseUrl)
    if ($KeepData) { $nodeArgs += "--keep-data" }
    & node @nodeArgs
    Track "社交 E2E 场景（test_social_e2e.js）" $LASTEXITCODE
}

# ── Phase 2b: 私聊 E2E ──
if (-not $SkipE2E -and $healthOk) {
    Header "Phase 2b: 私聊 E2E（Node）"
    $chatE2eScript = Join-Path $root "pocketbase\tools\test_social_chat_e2e.js"
    if (Test-Path $chatE2eScript) {
        $chatArgs = @($chatE2eScript, "--base-url", $BaseUrl)
        if ($KeepData) { $chatArgs += "--keep-data" }
        & node @chatArgs
        Track "私聊 E2E 场景（test_social_chat_e2e.js）" $LASTEXITCODE
    } else {
        Track "test_social_chat_e2e.js 存在" 1 "文件缺失"
    }
}

# ── Phase 3: Kotlin 纯逻辑单元测试 ──
if (-not $SkipKotlin) {
    Header "Phase 3: Kotlin 社交单元测试"
    $gradleArgs = if ($CI) { "--no-daemon -q --console=plain" } else { "--no-daemon -q" }
    Invoke-Expression ".\gradlew.bat testDebugUnitTest --tests com.example.funlife.social.* $gradleArgs" 2>&1 | Out-Null
    Track "Kotlin social.* 单元测试" $LASTEXITCODE

    $xml = Get-ChildItem "$root\app\build\test-results\testDebugUnitTest\TEST-com.example.funlife.social.*.xml" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($xml) {
        try {
            [xml]$doc = Get-Content $xml.FullName
            Info "  · $($doc.testsuite.name): $($doc.testsuite.tests) tests / $($doc.testsuite.failures) fail"
        } catch {}
    }
}

# ── 汇总 ──
$elapsed = ((Get-Date) - $startTime).TotalSeconds
$failed = ($results | Where-Object { -not $_.Pass }).Count
Header "汇总"
foreach ($r in $results) {
    if ($r.Pass) { Ok $r.Stage } else { Bad "$($r.Stage)  $($r.Detail)" }
}
if (-not $CI) {
    Write-Host ""
    if ($failed -eq 0) {
        Write-Host "  ★ 社交测试全部通过 · $([math]::Round($elapsed, 1))s" -ForegroundColor Green
    } else {
        Write-Host "  $failed 阶段失败 · $([math]::Round($elapsed, 1))s" -ForegroundColor Red
    }
}

if ($failed -gt 0) { exit 1 }
exit 0
