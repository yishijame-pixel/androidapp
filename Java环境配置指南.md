# ☕ Java 环境配置完整指南（Windows）

## 方法一：使用 Android Studio 自带的 JDK（最简单，推荐）

如果你已经安装了 Android Studio，它自带了 JDK，不需要单独安装！

### 步骤 1：找到 Android Studio 的 JDK 路径

1. 打开 Android Studio
2. 点击菜单：**File → Project Structure**
3. 左侧选择 **SDK Location**
4. 查看 **JDK location** 路径，通常是：
   ```
   C:\Program Files\Android\Android Studio\jbr
   ```
   或
   ```
   C:\Users\你的用户名\AppData\Local\Android\Sdk\jdk
   ```

### 步骤 2：设置 JAVA_HOME 环境变量

#### 方法 A：图形界面设置（推荐）

1. **打开环境变量设置**
   - 按 `Win + R` 键
   - 输入：`sysdm.cpl`
   - 按回车

2. **添加 JAVA_HOME**
   - 点击 "高级" 标签
   - 点击 "环境变量" 按钮
   - 在 "系统变量" 区域，点击 "新建"
   - 变量名：`JAVA_HOME`
   - 变量值：粘贴你在步骤1找到的 JDK 路径
   - 例如：`C:\Program Files\Android\Android Studio\jbr`
   - 点击 "确定"

3. **添加到 Path**
   - 在 "系统变量" 中找到 `Path`
   - 点击 "编辑"
   - 点击 "新建"
   - 输入：`%JAVA_HOME%\bin`
   - 点击 "确定"
   - 再次点击 "确定" 关闭所有窗口

4. **验证设置**
   - 打开新的 PowerShell 窗口（必须是新窗口）
   - 运行：`java -version`
   - 应该显示 Java 版本信息

#### 方法 B：使用 PowerShell 命令（快速）

以管理员身份运行 PowerShell，然后执行：

```powershell
# 设置 JAVA_HOME（替换为你的实际路径）
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "Machine")

# 添加到 Path
$path = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
$newPath = $path + ";%JAVA_HOME%\bin"
[System.Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")

Write-Host "环境变量设置完成！请重启 PowerShell 窗口" -ForegroundColor Green
```

---

## 方法二：下载并安装 JDK（如果没有 Android Studio）

### 步骤 1：下载 JDK

推荐下载 **JDK 17**（长期支持版本）

#### 选项 A：Oracle JDK
1. 访问：https://www.oracle.com/java/technologies/downloads/
2. 选择 **Java 17**
3. 下载 **Windows x64 Installer** (jdk-17_windows-x64_bin.exe)

#### 选项 B：OpenJDK（免费，推荐）
1. 访问：https://adoptium.net/
2. 选择 **JDK 17 (LTS)**
3. 选择 **Windows x64**
4. 点击下载 **.msi** 安装包

### 步骤 2：安装 JDK

1. 双击下载的安装包
2. 点击 "下一步"
3. 记住安装路径（默认是 `C:\Program Files\Java\jdk-17`）
4. 完成安装

### 步骤 3：配置环境变量

按照 **方法一 → 步骤 2** 的说明配置环境变量，但使用你的 JDK 安装路径。

---

## 验证安装

打开**新的** PowerShell 窗口，运行以下命令：

```powershell
# 检查 Java 版本
java -version

# 检查 javac 编译器
javac -version

# 检查 JAVA_HOME
echo $env:JAVA_HOME
```

**预期输出：**
```
java version "17.0.x" ...
javac 17.0.x
C:\Program Files\Android\Android Studio\jbr
```

---

## 常见问题解决

### 问题 1：命令提示符找不到 java

**原因：** 环境变量未生效

**解决：**
1. 完全关闭所有 PowerShell/CMD 窗口
2. 重新打开新窗口
3. 如果还不行，重启电脑

### 问题 2：JAVA_HOME 设置后仍然报错

**检查：**
```powershell
# 检查 JAVA_HOME 是否正确
Test-Path $env:JAVA_HOME

# 检查 java.exe 是否存在
Test-Path "$env:JAVA_HOME\bin\java.exe"
```

如果返回 `False`，说明路径不正确，重新设置。

### 问题 3：多个 Java 版本冲突

**解决：**
1. 确保 JAVA_HOME 指向你想用的版本
2. 在 Path 中，确保 `%JAVA_HOME%\bin` 在最前面

---

## 快速配置脚本

将以下内容保存为 `setup-java.ps1`，以管理员身份运行：

```powershell
# 自动查找 Android Studio 的 JDK
$possiblePaths = @(
    "C:\Program Files\Android\Android Studio\jbr",
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\jdk-11"
)

$jdkPath = $null
foreach ($path in $possiblePaths) {
    if (Test-Path "$path\bin\java.exe") {
        $jdkPath = $path
        Write-Host "找到 JDK: $jdkPath" -ForegroundColor Green
        break
    }
}

if ($jdkPath) {
    # 设置 JAVA_HOME
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkPath, "Machine")
    
    # 添加到 Path
    $path = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($path -notlike "*%JAVA_HOME%\bin*") {
        $newPath = "%JAVA_HOME%\bin;" + $path
        [System.Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
    }
    
    Write-Host "✓ JAVA_HOME 设置完成: $jdkPath" -ForegroundColor Green
    Write-Host "✓ Path 更新完成" -ForegroundColor Green
    Write-Host ""
    Write-Host "请重启 PowerShell 窗口，然后运行: java -version" -ForegroundColor Yellow
} else {
    Write-Host "✗ 未找到 JDK，请先安装 Android Studio 或 JDK" -ForegroundColor Red
}
```

---

## 配置完成后

环境变量配置完成后，你就可以：

1. ✅ 运行 Gradle 命令
2. ✅ 在 Android Studio 中编译项目
3. ✅ 使用命令行构建 APK

**下一步：** 返回项目目录，运行：
```powershell
./gradlew.bat --version
```

如果显示 Gradle 版本信息，说明配置成功！

---

## 需要帮助？

如果遇到问题，请提供以下信息：

```powershell
# 运行这些命令并提供输出
java -version
echo $env:JAVA_HOME
echo $env:Path
```
