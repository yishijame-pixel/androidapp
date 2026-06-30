# 扫描 dist/pac_maze_skins 下 anim_manifest.json，检查 walk 帧 fy/ty/高度一致性
param(
    [string]$Root = (Join-Path (Split-Path $PSScriptRoot -Parent) "dist\pac_maze_skins")
)

$ErrorActionPreference = "Stop"
$ReportFile = Join-Path (Split-Path $PSScriptRoot -Parent) "sprite_resource_audit.txt"

function Analyze-Manifest([string]$Path) {
    $json = Get-Content $Path -Raw -Encoding UTF8 | ConvertFrom-Json
    $skinId = $json.skinId
    $normalized = [bool]$json.normalized
    $anchorY = if ($json.anchorFrac) { [double]$json.anchorFrac.y } else { $null }
    $walk = $json.platformerMetrics.walk
    if (-not $walk) { return $null }
    $fys = @($walk | ForEach-Object { [double]$_.fy })
    $tys = @($walk | ForEach-Object { [double]$_.ty })
    $heights = @()
    for ($i = 0; $i -lt $walk.Count; $i++) {
        $heights += [double]$walk[$i].fy - [double]$walk[$i].ty
    }
    $fyMin = ($fys | Measure-Object -Minimum).Minimum
    $fyMax = ($fys | Measure-Object -Maximum).Maximum
    $tyMin = ($tys | Measure-Object -Minimum).Minimum
    $tyMax = ($tys | Measure-Object -Maximum).Maximum
    $hMin = ($heights | Measure-Object -Minimum).Minimum
    $hMax = ($heights | Measure-Object -Maximum).Maximum
    $outliers = @()
    for ($i = 0; $i -lt $walk.Count; $i++) {
        $fyGap = $fyMax - [double]$walk[$i].fy
        $tyGap = [double]$walk[$i].ty - $tyMin
        if ($fyGap -gt 0.08 -or $tyGap -gt 0.12 -or $heights[$i] -lt ($hMax * 0.75)) {
            $outliers += "frame$i(fy=$([math]::Round($walk[$i].fy,3)) ty=$([math]::Round($walk[$i].ty,3)) h=$([math]::Round($heights[$i],3)))"
        }
    }
    [PSCustomObject]@{
        SkinId = $skinId
        Normalized = $normalized
        AnchorY = $anchorY
        FrameCount = $walk.Count
        FyRange = [math]::Round($fyMax - $fyMin, 4)
        TyRange = [math]::Round($tyMax - $tyMin, 4)
        OpaqueHRange = [math]::Round($hMax - $hMin, 4)
        FyMin = [math]::Round($fyMin, 4)
        FyMax = [math]::Round($fyMax, 4)
        OutlierFrames = ($outliers -join "; ")
        Risk = if ($fyMax - $fyMin -gt 0.06 -or $tyMax - $tyMin -gt 0.08) { "HIGH" } elseif ($fyMax - $fyMin -gt 0.03) { "MED" } else { "LOW" }
    }
}

"" | Out-File $ReportFile -Encoding utf8
Add-Content $ReportFile "Sprite Resource Audit $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -Encoding utf8
Add-Content $ReportFile "Root: $Root" -Encoding utf8
Add-Content $ReportFile "" -Encoding utf8

$results = @()
Get-ChildItem $Root -Recurse -Filter "anim_manifest.json" | ForEach-Object {
    $r = Analyze-Manifest $_.FullName
    if ($r) { $results += $r }
}

$results = $results | Sort-Object { 
    switch ($_.Risk) { "HIGH" { 0 } "MED" { 1 } default { 2 } }
}, FyRange -Descending

Add-Content $ReportFile ("{0,-28} {1,-5} {2,-6} {3,-8} {4,-8} {5,-10} {6}" -f "SKIN", "NORM", "FRAMES", "FY_RNG", "TY_RNG", "OPQ_RNG", "RISK") -Encoding utf8
foreach ($r in $results) {
    $line = "{0,-28} {1,-5} {2,-6} {3,-8} {4,-8} {5,-10} {6}" -f $r.SkinId, $r.Normalized, $r.FrameCount, $r.FyRange, $r.TyRange, $r.OpaqueHRange, $r.Risk
    Add-Content $ReportFile $line -Encoding utf8
    if ($r.OutlierFrames) {
        Add-Content $ReportFile "  outliers: $($r.OutlierFrames)" -Encoding utf8
    }
}

Write-Host "Report: $ReportFile"
Get-Content $ReportFile
