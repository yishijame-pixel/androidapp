@echo off
chcp 65001 >nul
cls
echo.
echo ========================================
echo   ADB 连接修复工具
echo ========================================
echo.
echo 正在尝试修复 ADB 连接...
echo.

echo [步骤 1] 停止 ADB 服务器...
D:\Androidsdk\platform-tools\adb.exe kill-server
timeout /t 2 /nobreak >nul

echo [步骤 2] 启动 ADB 服务器...
D:\Androidsdk\platform-tools\adb.exe start-server
timeout /t 3 /nobreak >nul

echo [步骤 3] 检测设备...
D:\Androidsdk\platform-tools\adb.exe devices -l
echo.

echo ========================================
echo 请检查上面的设备列表：
echo ========================================
echo.
echo 如果看到你的手机设备 ID (3URNU24B26102576)：
echo   - 状态是 "device" → 连接成功！
echo   - 状态是 "offline" → 需要在手机上重新授权
echo   - 状态是 "unauthorized" → 需要在手机上点击"允许"
echo.
echo 如果没有看到手机设备：
echo   1. 检查 USB 线是否插好
echo   2. 下拉手机通知栏，点击"USB 连接"
echo   3. 选择"文件传输(MTP)"或"传输文件"
echo   4. 不要选"仅充电"
echo   5. 重新运行此脚本
echo.
echo 如果手机品牌是小米/华为/OPPO：
echo   - 可能需要在开发者选项中开启额外的权限
echo   - 小米：开启"USB 安装"和"USB 调试（安全设置）"
echo   - 华为：开启"仅充电模式下允许 ADB 调试"
echo.
pause
