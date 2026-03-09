@echo off
echo 正在编译应用...
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
call gradlew.bat :app:assembleDebug --no-daemon --stacktrace
if %ERRORLEVEL% EQU 0 (
    echo 编译成功！
    echo 正在安装...
    D:\Androidsdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
    if %ERRORLEVEL% EQU 0 (
        echo 安装成功！
    ) else (
        echo 安装失败！
    )
) else (
    echo 编译失败！
)
pause
