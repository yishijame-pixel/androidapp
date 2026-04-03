@echo off
echo ========================================
echo 清理并重新安装 FunLife App
echo ========================================
echo.

set ADB_PATH=D:\Androidsdk\platform-tools\adb.exe

if not exist "%ADB_PATH%" (
    echo 错误: 找不到 ADB，路径: %ADB_PATH%
    echo 请检查 Android SDK 安装路径
    pause
    exit /b 1
)

echo 步骤 1: 启动 ADB 服务...
"%ADB_PATH%" start-server
timeout /t 2 /nobreak >nul

echo.
echo 步骤 2: 检查连接的设备...
"%ADB_PATH%" devices
echo.

echo 步骤 3: 卸载旧版本应用（清除所有数据）...
echo 从真机卸载...
"%ADB_PATH%" -s 3URNU24B26102576 uninstall com.example.funlife
echo 从模拟器卸载...
"%ADB_PATH%" -s emulator-5554 uninstall com.example.funlife
echo.

echo 步骤 4: 编译新版本...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo ❌ 编译失败！
    pause
    exit /b 1
)
echo.

echo 步骤 5: 安装新版本到真机...
"%ADB_PATH%" -s 3URNU24B26102576 install app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo.
    echo ❌ 真机安装失败！
    echo 尝试安装到模拟器...
    "%ADB_PATH%" -s emulator-5554 install app\build\outputs\apk\debug\app-debug.apk
    if errorlevel 1 (
        echo ❌ 模拟器安装也失败！
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo ✅ 清理并安装完成！
echo ========================================
echo.
echo 现在可以测试：
echo 1. 注册新账号
echo 2. 测试输入法是否正常
echo 3. 添加第一个纪念日，观察是否立即显示
echo.
pause
