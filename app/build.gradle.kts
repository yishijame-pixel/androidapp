import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "com.example.funlife"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.funlife"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 配置从 local.properties 注入（该文件不进 git）
        val localProps = File(rootProject.projectDir, "local.properties")
        val props = Properties().apply {
            if (localProps.exists()) load(FileInputStream(localProps))
        }
        buildConfigField("String", "AI_API_KEY", "\"${props.getProperty("AI_API_KEY", "")}\"")
        // 🔒 AI 提供商技术参数从 local.properties 注入，不在源码 / 不在编译产物里硬编码厂商名
        //   必须在 local.properties 配置：
        //     AI_BASE_URL=https://api.<供应商>.com/
        //     AI_MODEL=<模型名>
        //   未配置时 baseUrl 会落到 https://localhost/ → 网络调用失败 → 优雅降级到本地规则引擎
        buildConfigField("String", "AI_BASE_URL", "\"${props.getProperty("AI_BASE_URL", "")}\"")
        buildConfigField("String", "AI_MODEL", "\"${props.getProperty("AI_MODEL", "")}\"")
        buildConfigField("String", "VIP_BACKEND_URL", "\"${props.getProperty("VIP_BACKEND_URL", "")}\"")
        buildConfigField("String", "VIP_HMAC_SECRET", "\"${props.getProperty("VIP_HMAC_SECRET", "")}\"")
        // 🔒 Cert Pinning：云函数证书的 SHA-256 指纹（多个用逗号分隔，如 "sha256/AAA=,sha256/BBB="）
        //    获取方法：openssl s_client -connect <你的域名>:443 -servername <域名> < /dev/null \
        //               | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der \
        //               | openssl dgst -sha256 -binary | openssl enc -base64
        //    建议 pin 主证书 + 备用证书，避免轮换时全量用户失联。
        buildConfigField("String", "VIP_BACKEND_PIN", "\"${props.getProperty("VIP_BACKEND_PIN", "")}\"")
        // 🔒 应用签名指纹（SHA-256，大写 HEX，无冒号）。留空 = 跳过自校验。
        //    获取：keytool -list -v -keystore <release.jks> | findstr SHA256
        //    建议在 release 构建时设置：在 local.properties 添加 APP_SIGN_SHA256=...
        buildConfigField("String", "APP_SIGN_SHA256", "\"${props.getProperty("APP_SIGN_SHA256", "")}\"")
        // 🆕 v51 时光信箱 AI 代理灰度开关
        //   true  = 优先走云函数 /letter_ai（KEY 在云端，安全 / 服务端权威配额）；失败回退直连
        //   false = 仅走客户端直连 AI（开发态调试 / 自建 LLM 时）
        //   默认 true。可在 local.properties 加 LETTER_AI_USE_PROXY=false 关闭。
        buildConfigField(
            "boolean", "LETTER_AI_USE_PROXY",
            props.getProperty("LETTER_AI_USE_PROXY", "true")
        )
        // 🆕 v51 聊天记账 AI 代理灰度开关（同 LETTER_AI_USE_PROXY）
        buildConfigField(
            "boolean", "CHAT_AI_USE_PROXY",
            props.getProperty("CHAT_AI_USE_PROXY", "true")
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // R8 完整模式：更激进的混淆/裁剪（包体进一步减小 5–10%）
            // 注：所有反射点已在 proguard-rules.pro 加了 -keep
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        // 升 Java 11：SceneView/Filament 2.x 必须
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 📖 真实 3D 翻页（OpenGL ES 2.0，本地模块，源自 eschao/android-PageFlip Apache-2.0）
    implementation(project(":PageFlip"))

    // ✨ SceneView · Google Filament 的 Compose 封装。
    //   用于魔法书 widget 的企业级 PBR 3D 渲染。
    implementation("io.github.sceneview:sceneview:2.2.1")

    // 🎬 Lottie · Airbnb 的 After Effects 动画播放器（Compose 版）。
    //   用于皮肤特效（火焰/雷电/樱花/流星），从 LottieFiles 加载专业级 JSON 动画。
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")  // 🔥 GIF支持
    
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    // 进程级生命周期：监听 App 整体前台/后台切换（用于回到前台时刷新 VIP 配置等）
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Security - EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // 🔔 WorkManager - 后台定期检查纪念日（更可靠，能抗 OEM 后台杀进程）
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Retrofit + Gson (AI Service / 数据备份)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ════════════════════════════════════════════════════════════
    // 🧪 测试依赖 — JVM 单元 + Robolectric（无需真机即可跑 Room）
    // ════════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("com.google.truth:truth:1.1.5")

    // Instrumented (真机) — 仅占位，跑 unit 测试时不会拉取
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// 单元测试启用 Robolectric + Compose 选项
tasks.withType<Test> {
    useJUnit()
    systemProperty("robolectric.enabledSdks", "33")
    systemProperty("robolectric.logging", "stdout")
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}
