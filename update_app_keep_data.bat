@echo off
echo ========================================
echo 更新应用（保留数据和账号）
echo ========================================
echo.
echo [1/2] 编译新的APK...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo.
echo [2/2] 安装到手机（保留数据）...
D:\Androidsdk\platform-tools\adb.exe -s 3URNU24B26102576 install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo 安装失败！
    pause
    exit /b 1
)
echo.
echo ========================================
echo 更新完成！账号和数据已保留
echo ========================================
pause
