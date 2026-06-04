# 启动 PocketBase 开发服务（监听所有网卡，供真机/模拟器访问）
Set-Location $PSScriptRoot

$pushEnv = Join-Path $PSScriptRoot "secrets\push.env"
if (Test-Path $pushEnv) {
    Get-Content $pushEnv | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            $name = $Matches[1].Trim()
            $val = $Matches[2].Trim()
            if ($name -eq "FCM_RELAY_URL" -or $name -eq "FCM_RELAY_KEY") {
                Set-Item -Path "env:$name" -Value $val
                Write-Host "env $name = $val"
            }
        }
    }
}

Write-Host "PocketBase -> http://127.0.0.1:8090"
Write-Host "管理后台 -> http://127.0.0.1:8090/_/"
.\pocketbase.exe serve --http=0.0.0.0:8090
