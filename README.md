# FunLife - 生活娱乐工具 App

一个基于 Jetpack Compose 和 Material Design 3 的 Android 应用，包含纪念日管理、幸运转盘和游戏计分三大功能模块。

## 📱 功能特性

### 1️⃣ 纪念日管理
- ✅ 添加/删除纪念日
- ✅ 查看纪念日列表
- ✅ 自动计算剩余天数
- ✅ 美观的卡片式展示

### 2️⃣ 幸运转盘
- ✅ 可自定义选项的转盘
- ✅ 流畅的旋转动画
- ✅ 随机选择结果
- ✅ 支持编辑转盘选项

### 3️⃣ 游戏计分
- ✅ 添加/删除玩家
- ✅ 增加/减少分数
- ✅ 自动排行榜
- ✅ 一键重置所有分数

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **设计规范**: Material Design 3
- **架构**: MVVM
- **导航**: Navigation Compose
- **数据存储**: Room Database
- **最低版本**: Android 8.0 (API 26)

## 📂 项目结构

```
app/
├── data/
│   ├── model/
│   │   ├── Anniversary.kt      # 纪念日数据模型
│   │   └── Player.kt           # 玩家数据模型
│   ├── dao/
│   │   ├── AnniversaryDao.kt   # 纪念日数据访问
│   │   └── PlayerDao.kt        # 玩家数据访问
│   └── database/
│       └── AppDatabase.kt      # Room 数据库
├── repository/
│   ├── AnniversaryRepository.kt
│   └── PlayerRepository.kt
├── viewmodel/
│   ├── AnniversaryViewModel.kt
│   └── ScoreViewModel.kt
├── ui/
│   ├── screens/
│   │   ├── AnniversaryScreen.kt    # 纪念日页面
│   │   ├── SpinWheelScreen.kt      # 转盘页面
│   │   └── ScoreCounterScreen.kt   # 计分页面
│   ├── components/
│   │   ├── AnniversaryCard.kt      # 纪念日卡片
│   │   ├── PlayerCard.kt           # 玩家卡片
│   │   └── SpinWheel.kt            # 转盘组件
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── navigation/
│   └── NavGraph.kt             # 导航配置
└── MainActivity.kt             # 主活动
```

## 🚀 运行步骤

### 1. 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 8 或更高版本
- Android SDK API 26+

### 2. 创建项目
1. 打开 Android Studio
2. 选择 "New Project"
3. 选择 "Empty Activity"
4. 配置项目：
   - Name: `FunLife`
   - Package name: `com.example.funlife`
   - Language: `Kotlin`
   - Minimum SDK: `API 26: Android 8.0 (Oreo)`

### 3. 复制代码
1. 将所有生成的文件复制到对应目录
2. 确保 `build.gradle.kts` 文件正确配置
3. 同步 Gradle 文件 (Sync Now)

### 4. 运行应用
1. 连接 Android 设备或启动模拟器
2. 点击 Run 按钮 (绿色三角形)
3. 等待编译完成
4. 应用将自动安装并启动

## 📝 使用说明

### 纪念日管理
1. 点击右下角 ➕ 按钮
2. 输入纪念日名称和日期
3. 点击确定保存
4. 卡片会显示剩余天数
5. 点击删除图标可删除纪念日

### 幸运转盘
1. 点击编辑按钮自定义选项
2. 每行输入一个选项
3. 点击"开始旋转"按钮
4. 等待转盘停止查看结果

### 游戏计分
1. 点击 ➕ 添加玩家
2. 使用 ➕/➖ 按钮调整分数
3. 自动按分数排序
4. 点击刷新按钮重置所有分数

## 🎨 UI 特点

- 现代化 Material Design 3 设计
- 流畅的动画效果
- 响应式布局
- 直观的用户交互
- 中文界面

## 📦 依赖项

```kotlin
// Compose
implementation(platform("androidx.compose:compose-bom:2023.10.01"))
implementation("androidx.compose.material3:material3")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.5")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
```

## 🔧 故障排除

### Gradle 同步失败
- 检查网络连接
- 更新 Gradle 版本
- 清理项目: Build → Clean Project

### 编译错误
- 确保所有文件路径正确
- 检查包名是否一致
- 重新同步 Gradle

### 运行时错误
- 检查最低 SDK 版本
- 确保设备/模拟器版本 ≥ API 26
- 查看 Logcat 日志

## 📄 许可证

本项目仅供学习和演示使用。

## 👨‍💻 开发者

使用 Kotlin + Jetpack Compose 构建
