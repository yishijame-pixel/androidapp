# Java 环境自动配置脚本
# 以管理员身份运行此脚本

Write-Host "=== Java 环境自动配置工具 ===" -ForegroundColor Cyan
Write-Host ""

# 检查是否以管理员身份运行
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "✗ 请以管理员身份运行此脚本！" -ForegroundColor Red
    Write-Host ""
    Write-Host "右键点击 PowerShell → 选择 '以管理员身份运行'" -ForegroundColor Yellow
    pause
    exit
}

Write-Host "✓ 管理员权限确认" -ForegroundColor Green
Write-Host ""

# 查找 JDK
Write-Host "正在查找 JDK..." -ForegroundColor Yellow

$possiblePaths = @(
    "C:\Program Files\Android\Android Studio\jbr",
    "C:\Program Files\Android\Android Studio\jre",
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
    "$env:LOCALAPPDATA\Android\Sdk\jdk",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\jdk-11",
    "C:\Program Files\Java\jdk1.8.0_*",
    "C:\Program Files\Eclipse Adoptium\jdk-17*"
)

$jdkPath = $null
foreach ($path in $possiblePaths) {
    # 支持通配符
    $expandedPaths = Get-Item $path -ErrorAction SilentlyContinue
    foreach ($expandedPath in $expandedPaths) {
        if (Test-Path "$expandedPath\bin\java.exe") {
            $jdkPath = $expandedPath.FullName
            Write-Host "✓ 找到 JDK: $jdkPath" -ForegroundColor Green
            break
        }
    }
    if ($jdkPath) { break }
}

if (-not $jdkPath) {
    Write-Host "✗ 未找到 JDK" -ForegroundColor Red
    Write-Host ""
    Write-Host "请先安装以下之一：" -ForegroundColor Yellow
    Write-Host "1. Android Studio (推荐) - https://developer.android.com/studio"
    Write-Host "2. JDK 17 - https://adoptium.net/"
    Write-Host ""
    pause
    exit
}

Write-Host ""

# 设置 JAVA_HOME
Write-Host "正在设置 JAVA_HOME..." -ForegroundColor Yellow
try {
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkPath, "Machine")
    Write-Host "✓ JAVA_HOME 设置成功: $jdkPath" -ForegroundColor Green
} catch {
    Write-Host "✗ 设置 JAVA_HOME 失败: $_" -ForegroundColor Red
    pause
    exit
}

Write-Host ""

# 更新 Path
Write-Host "正在更新 Path 环境变量..." -ForegroundColor Yellow
try {
    $path = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    
    # 移除旧的 JAVA_HOME 引用
    $path = $path -replace ";?%JAVA_HOME%\\bin;?", ""
    $path = $path -replace ";?[^;]*\\Java\\[^;]*\\bin;?", ""
    
    # 添加新的 JAVA_HOME
    $newPath = "%JAVA_HOME%\bin;" + $path
    [System.Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
    
    Write-Host "✓ Path 更新成功" -ForegroundColor Green
} catch {
    Write-Host "✗ 更新 Path 失败: $_" -ForegroundColor Red
    pause
    exit
}

Write-Host ""
Write-Host "=== 配置完成 ===" -ForegroundColor Green
Write-Host ""
Write-Host "请执行以下步骤：" -ForegroundColor Yellow
Write-Host "1. 关闭当前 PowerShell 窗口"
Write-Host "2. 打开新的 PowerShell 窗口"
Write-Host "3. 运行命令验证: java -version"
Write-Host ""
Write-Host "如果显示 Java 版本信息，说明配置成功！" -ForegroundColor Green
Write-Host ""

pause
