# 允许局域网设备访问 PocketBase 8090（需管理员 PowerShell）
netsh advfirewall firewall add rule name="PocketBase 8090" dir=in action=allow protocol=TCP localport=8090
Write-Host "Firewall rule added: TCP 8090 inbound allowed"
