@echo off
echo ========================================
echo 正在编译应用...
echo ========================================

REM 设置 JAVA_HOME
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

REM 编译
call gradlew.bat :app:assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo 编译成功！正在安装到模拟器...
echo ========================================

REM 安装
D:\Androidsdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% NEQ 0 (
    echo 安装失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo 安装成功！正在启动应用...
echo ========================================

REM 启动应用
D:\Androidsdk\platform-tools\adb.exe shell am start -n com.example.funlife/.MainActivity

echo.
echo ========================================
echo 完成！应用已启动
echo ========================================
pause
