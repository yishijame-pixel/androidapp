@echo off
chcp 65001 >nul
echo ========================================
echo 清理应用数据并重新安装
echo ========================================
echo.

echo [1/4] 清理旧的构建文件...
call gradlew.bat clean
if errorlevel 1 (
    echo 清理失败！
    pause
    exit /b 1
)

echo.
echo [2/4] 编译新的APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo [3/4] 卸载旧版本应用（清除所有数据）...
D:\Androidsdk\platform-tools\adb.exe uninstall com.example.funlife
echo 旧版本已卸载

echo.
echo [4/4] 安装新版本...
D:\Androidsdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
    echo 安装失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 安装成功！数据库已重置
echo ========================================
echo.
echo 提示：
echo - 所有旧数据已清除
echo - 数据库已升级到最新版本
echo - 可以在模拟器上测试了
echo.
pause
