# 启动 PocketBase 开发服务（监听所有网卡，供真机/模拟器访问）
Set-Location $PSScriptRoot
Write-Host "PocketBase -> http://127.0.0.1:8090"
Write-Host "管理后台 -> http://127.0.0.1:8090/_/"
.\pocketbase.exe serve --http=0.0.0.0:8090
