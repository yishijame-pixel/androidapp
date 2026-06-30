#Requires -Version 5.1
param(
    [switch]$SkipExport,
    [switch]$SkipImport,
    [switch]$SkipTunnelDns,
    [switch]$TcbOnly
)

$ErrorActionPreference = "Stop"
$pbDir = $PSScriptRoot | Split-Path -Parent
$repoRoot = Split-Path $pbDir -Parent
Set-Location $pbDir

function Write-Step($m) { Write-Host "`n>> $m" -ForegroundColor Cyan }

function Read-DotEnv($path) {
    $map = @{}
    if (-not (Test-Path $path)) { return $map }
    Get-Content $path | ForEach-Object {
        $line = $_
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
            if ($line.TrimStart().StartsWith("#")) { return }
            $map[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'")
        }
    }
    return $map
}

function Import-EnvMap($map) {
    foreach ($k in $map.Keys) {
        if (-not [Environment]::GetEnvironmentVariable($k)) {
            [Environment]::SetEnvironmentVariable($k, $map[$k])
        }
        if (-not (Get-Item -Path "env:$k" -ErrorAction SilentlyContinue)) {
            Set-Item -Path "env:$k" -Value $map[$k]
        }
    }
}

Import-EnvMap (Read-DotEnv (Join-Path $pbDir ".env"))
Import-EnvMap (Read-DotEnv (Join-Path $repoRoot "backend\tools\.env"))
$toolsEnvPath = Join-Path $repoRoot "backend\tools\.env"
if (Test-Path $toolsEnvPath) {
    $te = Read-DotEnv $toolsEnvPath
    foreach ($k in $te.Keys) { Set-Item -Path "env:$k" -Value $te[$k] -Force }
}
$pbEnvPath = Join-Path $pbDir ".env"
if (Test-Path $pbEnvPath) {
    $pe = Read-DotEnv $pbEnvPath
    foreach ($k in $pe.Keys) { Set-Item -Path "env:$k" -Value $pe[$k] -Force }
}

$localProps = Join-Path $repoRoot "local.properties"
if (Test-Path $localProps) {
    $hmac = Select-String -Path $localProps -Pattern "VIP_HMAC_SECRET=(.+)" | Select-Object -First 1
    if ($hmac -and -not $env:HMAC_SECRET) {
        $env:HMAC_SECRET = $hmac.Matches.Groups[1].Value.Trim()
    }
}

if (-not $env:AI_API_KEY) {
    $cb = Join-Path $repoRoot "backend\cloudbaserc.json"
    if (Test-Path $cb) {
        $m = Select-String -Path $cb -Pattern '"AI_API_KEY"\s*:\s*"([^"]+)"' | Select-Object -First 1
        if ($m) { $env:AI_API_KEY = $m.Matches.Groups[1].Value }
    }
}

if (-not $env:POSTGRES_PASSWORD) {
    $env:POSTGRES_PASSWORD = "funlife_" + ([guid]::NewGuid().ToString("N").Substring(0, 16))
}

if (-not $env:HMAC_SECRET) { throw "Missing HMAC_SECRET" }

if (-not $TcbOnly) {
    Write-Step "Pull postgres image"
    $pulled = $false
    foreach ($img in @("postgres:15-alpine", "docker.m.daocloud.io/library/postgres:15-alpine")) {
        docker pull $img 2>$null
        if ($LASTEXITCODE -eq 0) {
            if ($img -ne "postgres:15-alpine") { docker tag $img postgres:15-alpine 2>$null }
            $pulled = $true
            break
        }
    }
    if (-not $pulled) { throw "Cannot pull postgres image. Use -TcbOnly." }

    Write-Step "Start postgres"
    $ErrorActionPreference = "Continue"
    docker compose up -d postgres 2>&1 | Out-Null
    $ErrorActionPreference = "Stop"
    $pgOk = $false
    for ($i = 1; $i -le 30; $i++) {
        Start-Sleep -Seconds 2
        $h = docker inspect funlife-postgres --format "{{.State.Health.Status}}" 2>$null
        if ($h -eq "healthy") { $pgOk = $true; break }
    }
    if (-not $pgOk) { throw "Postgres not healthy" }

    Write-Step "Apply PG migrations"
    $pgUrl = "postgres://funlife:$($env:POSTGRES_PASSWORD)@postgres:5432/funlife_vip"
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    foreach ($migration in @("001_documents.sql", "002_admin_indexes.sql")) {
        $mPath = Join-Path $repoRoot "backend\migrations\postgres\$migration"
        if (Test-Path $mPath) {
            docker run --rm --network funlife-net `
                -v "${repoRoot}/backend:/app/backend:ro" `
                -e "PGPASSWORD=$($env:POSTGRES_PASSWORD)" `
                postgres:15-alpine `
                psql -h postgres -U funlife -d funlife_vip -v ON_ERROR_STOP=1 -f "/app/backend/migrations/postgres/$migration" 2>&1 | ForEach-Object { Write-Host $_ }
            if ($LASTEXITCODE -ne 0) {
                $ErrorActionPreference = $prevEap
                throw "Migration failed: $migration (exit $LASTEXITCODE)"
            }
        }
    }
    $ErrorActionPreference = $prevEap
}

if (-not $SkipExport -and -not $TcbOnly) {
    Write-Step "Export TCB (nosql RunCommands)"
    Push-Location (Join-Path $repoRoot "backend\tools")
    node export_tcb_nosql.js
    if ($LASTEXITCODE -ne 0) {
        Write-Host "nosql export failed, fallback export_tcb.js" -ForegroundColor Yellow
        node export_tcb.js
    }
    Pop-Location
}

if (-not $SkipImport -and -not $TcbOnly) {
    Write-Step "Import PG"
    $exportDir = Get-ChildItem (Join-Path $repoRoot "backup\tcb-export") -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    $pgUrl = "postgres://funlife:$($env:POSTGRES_PASSWORD)@postgres:5432/funlife_vip"
    if ($exportDir -and (Get-ChildItem $exportDir.FullName -Filter "*.json" | Where-Object { $_.Name -ne "_meta.json" })) {
        docker run --rm --network funlife-net -v "${repoRoot}/backend:/app/backend:ro" -v "${repoRoot}/backup:/app/backup:ro" -e "DATABASE_URL=$pgUrl" -e "NODE_PATH=/app/backend/tools/node_modules" node:20-alpine sh -c "cd /app/backend/tools && npm install pg --omit=dev --silent && NODE_PATH=/app/backend/tools/node_modules node import_pg.js /app/backup/tcb-export/$($exportDir.Name)"
    } else {
        docker run --rm --network funlife-net -v "${repoRoot}/backend:/app/backend" -e "DATABASE_URL=$pgUrl" -e "NODE_PATH=/app/backend/tools/node_modules" node:20-alpine sh -c "cd /app/backend/tools && npm install pg --omit=dev --silent && NODE_PATH=/app/backend/tools/node_modules node seed_pg_defaults.js"
    }
}

Write-Step "Build and start funlife-api"
docker compose --profile api build funlife-api

if ($TcbOnly) {
    docker rm -f funlife-api | Out-Null
    docker run -d --name funlife-api --network funlife-net --restart unless-stopped -p "3400:3400" `
        -e PORT=3400 -e HOST=0.0.0.0 `
        -e HMAC_SECRET=$env:HMAC_SECRET `
        -e AI_API_KEY=$env:AI_API_KEY `
        -e TCB_ENV_ID=$env:TCB_ENV_ID `
        -e TCB_SECRET_ID=$env:TCB_SECRET_ID `
        -e TCB_SECRET_KEY=$env:TCB_SECRET_KEY `
        funlife-funlife-api:latest
} else {
    docker rm -f funlife-api | Out-Null
    docker compose --profile api up -d funlife-api
    Write-Step "Recreate vip-admin (sync DATABASE_URL with postgres/api)"
    docker rm -f funlife-vip-admin | Out-Null
    docker compose --profile admin build vip-admin
    docker compose --profile admin up -d vip-admin
}

Write-Step "Restart cloudflared"
$ErrorActionPreference = "Continue"
docker compose --profile tunnel restart cloudflared 2>&1 | Out-Null
if (-not $SkipTunnelDns) {
    if (Get-Command cloudflared -ErrorAction SilentlyContinue) {
        cloudflared tunnel route dns funlife-pb api.yishi.site 2>&1 | Out-Null
    }
}
$ErrorActionPreference = "Stop"

Write-Step "Verify"
Start-Sleep -Seconds 5
Invoke-RestMethod "http://127.0.0.1:3400/health" -TimeoutSec 10 | ConvertTo-Json
Invoke-RestMethod "http://127.0.0.1:3400/vip_config" -Method POST -ContentType "application/json" -Body "{}" -TimeoutSec 15 | Select-Object ok
