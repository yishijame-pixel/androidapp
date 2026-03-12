# FunLife - Life Entertainment Tool Android App

[中文文档](项目文档.md) | English

## 📋 Project Overview

FunLife is an Android application built with Jetpack Compose and Material Design 3, integrating multiple practical life entertainment modules including user system, anniversary management, lucky wheel, game scoring, habit tracking, goal management, mood recording, shop system, and more.

### App Information
- **App Name**: FunLife
- **Package Name**: com.example.funlife
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM

---

## 🎯 Core Features

### 1. User System 👤
- ✅ User registration (requires beta code: 223498)
- ✅ User login
- ✅ Session management (auto-login)
- ✅ User profile management
- ✅ Cute-style login/register UI

### 2. Anniversary Management 📅
- ✅ Add/edit/delete anniversaries
- ✅ Auto-calculate remaining or elapsed days
- ✅ Anniversary statistics
- ✅ Beautiful card-style display
- ✅ Support different anniversary types

### 3. Lucky Wheel 🎡
- ✅ Multiple wheel modes (Normal, Rare, Epic, Legendary)
- ✅ Customizable wheel options
- ✅ Guarantee mechanism (gacha-like system)
- ✅ Lucky value system
- ✅ Spin history
- ✅ Multi-spin feature
- ✅ Smooth rotation animation
- ✅ Wheel template management

### 4. Game Scoring 🎮
- ✅ Add/remove players
- ✅ Increase/decrease scores
- ✅ Auto-ranking leaderboard
- ✅ One-click reset all scores
- ✅ Game history

### 5. Habit Tracking ✅
- ✅ Create and manage habits
- ✅ Daily check-in records
- ✅ Habit completion statistics
- ✅ Habit streak tracking

### 6. Goal Management 🎯
- ✅ Set short-term/long-term goals
- ✅ Goal progress tracking
- ✅ Goal completion status
- ✅ Goal categorization

### 7. Mood Recording 😊
- ✅ Record daily mood
- ✅ Mood statistics
- ✅ Mood trend charts
- ✅ Mood diary

### 8. Shop System 🛒
- ✅ Virtual coin system
- ✅ Item purchase
- ✅ Purchase history
- ✅ Coin transaction records

### 9. Data Statistics 📊
- ✅ Module data statistics
- ✅ Visual chart display
- ✅ History query
- ✅ Data export

### 10. Settings Center ⚙️
- ✅ User preferences
- ✅ Theme switching
- ✅ Data backup & restore
- ✅ App information

---

## 🛠️ Tech Stack

### Core Technologies
- **Kotlin**: Primary development language
- **Jetpack Compose**: Modern UI framework
- **Material Design 3**: UI design specification
- **Coroutines**: Asynchronous programming
- **Flow**: Reactive data streams

### Jetpack Components
- **Room Database**: Local data persistence
- **ViewModel**: View model management
- **Navigation Compose**: Navigation management
- **Lifecycle**: Lifecycle management

### Third-party Libraries
- **Coil**: Image loading library
- **Material Icons Extended**: Extended icon library

---

## 🚀 Quick Start

### Requirements
- **Android Studio**: Hedgehog (2023.1.1) or higher
- **JDK**: 8 or higher (JDK 17 recommended)
- **Android SDK**: API 26+
- **Gradle**: 8.1.4
- **Kotlin**: 1.9.20

### Clone Project
```bash
git clone <repository-url>
cd FunLife
```

### Configure SDK Path
Create or edit `local.properties` in project root:
```properties
sdk.dir=<your-android-sdk-path>
```

Example:
```properties
sdk.dir=D\:\\Androidsdk
```

### Run with Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Select project root directory
4. Wait for Gradle sync to complete
5. Click "Run" button (green triangle) or press `Shift+F10`
6. Select emulator or connected device
7. App will compile, install, and launch automatically

### Build with Command Line

#### Windows

1. **Build Debug Version**
```bash
gradlew.bat clean assembleDebug
```

2. **Install to Device**
```bash
<SDK-path>\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

3. **Launch App**
```bash
<SDK-path>\platform-tools\adb.exe shell am start -n com.example.funlife/.MainActivity
```

#### Use Provided Batch Scripts

```bash
# Compile app
compile.bat

# Compile, install and launch
build-and-install.bat
```

---

## 📱 User Guide

### First Time Use

1. **Launch App**
   - Welcome screen appears on first launch

2. **Register Account**
   - Click "Register" button
   - Enter username (min 3 characters)
   - Enter nickname (optional)
   - Set password (min 6 characters)
   - Enter beta code: `223498`
   - Click "Register"

3. **Login**
   - Enter username and password
   - Click "Login"
   - App remembers login state

### Feature Usage

#### Anniversary Management
1. Navigate to Anniversary page
2. Click ➕ button at bottom right
3. Enter anniversary name and date
4. Select anniversary type
5. Save and auto-calculate remaining days

#### Lucky Wheel
1. Navigate to Wheel page
2. Select wheel mode (Normal/Rare/Epic/Legendary)
3. Click "Start Spin" button
4. Wait for wheel to stop and view result
5. Multi-spin supported (requires enough coins)

#### Game Scoring
1. Navigate to Score page
2. Click ➕ to add player
3. Use ➕/➖ buttons to adjust scores
4. Auto-sort by score
5. Click refresh to reset all scores

---

## 💾 Database Design

### Room Database Version: 12

### Tables

1. **users** - User table
2. **anniversaries** - Anniversary table
3. **players** - Player table
4. **game_history** - Game history table
5. **spin_wheel_templates** - Wheel template table
6. **spin_wheel_history** - Spin history table
7. **custom_spin_modes** - Custom spin mode table
8. **guarantee_counters** - Guarantee counter table
9. **habits** - Habit table
10. **goals** - Goal table
11. **mood_entries** - Mood entry table
12. **shop_items** - Shop item table
13. **user_coins** - User coins table
14. **user_preferences** - User preferences table

---

## 🔧 Configuration

### Dependencies

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    
    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")
}
```

### Permissions

```xml
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. Gradle Sync Failed
**Solution**:
- Check network connection
- Use Aliyun mirror (configured in settings.gradle.kts)
- Clean project: `Build → Clean Project`
- Re-sync: `File → Sync Project with Gradle Files`

#### 2. Compilation Error
**Solution**:
- Ensure correct JDK version (JDK 17 recommended)
- Check SDK path configuration
- Delete `.gradle` and `build` folders, then rebuild

#### 3. Database Version Conflict
**Solution**:
- Uninstall and reinstall app
- Or use database migration strategy

---

## 📊 Performance Optimization

### Implemented Optimizations

1. **Database Optimization**
   - Use Room Flow for reactive data
   - Proper indexing
   - Batch operation optimization

2. **UI Optimization**
   - LazyColumn/LazyRow for lazy loading
   - Use remember to cache computed results
   - Avoid unnecessary recomposition

3. **Memory Optimization**
   - Timely resource release
   - Coil image caching
   - Avoid memory leaks

4. **Build Optimization**
   - Enable Gradle daemon
   - Enable parallel compilation
   - Enable build cache

---

## 🔐 Security

### Current Implementation
- Plain text password storage (demo only)
- Session management with SharedPreferences

### Production Recommendations
1. **Password Encryption**: Use BCrypt or similar
2. **Data Encryption**: Use SQLCipher for database
3. **Network Security**: Use HTTPS
4. **Token Authentication**: Implement JWT or OAuth

---

## 📝 Development Log

### Completed Features
- ✅ User system (register, login, session)
- ✅ Anniversary management
- ✅ Lucky wheel (multi-mode, guarantee, multi-spin)
- ✅ Game scoring
- ✅ Habit tracking
- ✅ Goal management
- ✅ Mood recording
- ✅ Shop system
- ✅ Data statistics
- ✅ Settings center

### Fixed Issues
- ✅ Wheel click not responding
- ✅ Coroutine scope error
- ✅ Multi-spin logic optimization
- ✅ Database version upgrade

### Planned Features
- ⏳ Password encryption
- ⏳ Cloud data sync
- ⏳ Social sharing
- ⏳ More theme options
- ⏳ Export to Excel/CSV
- ⏳ Notification reminders

---

## 📚 Documentation

Project includes detailed documentation:

1. **如何编译和运行.md** - Build and run guide (Chinese)
2. **命令行编译运行指南.md** - Command line guide (Chinese)
3. **用户系统集成指南.md** - User system integration (Chinese)
4. **修复说明.md** - Fix instructions (Chinese)
5. **项目文档.md** - Complete project documentation (Chinese)

---

## 🤝 Contributing

### Code Standards
- Follow Kotlin official coding conventions
- Use meaningful variable and function names
- Add necessary comments
- Keep code clean and concise

### Commit Convention
```
feat: Add new feature
fix: Fix bug
docs: Update documentation
style: Code formatting
refactor: Code refactoring
test: Add tests
chore: Build/toolchain updates
```

---

## 📄 License

This project is for learning and demonstration purposes only.

---

## 👨‍💻 Developer Info

- **Framework**: Jetpack Compose
- **Language**: Kotlin
- **Architecture**: MVVM
- **Database**: Room
- **Last Updated**: 2026-03-13

---

## 🎉 Acknowledgments

Thanks to these open source projects:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Coil](https://coil-kt.github.io/coil/)

---

**Last Updated**: 2026-03-13  
**Version**: 1.0.0
