// SecurityInitializer.kt - 安全系统初始化器
package com.example.funlife.security

import android.content.Context
import android.util.Log

/**
 * 安全系统初始化器
 * 
 * 在应用启动时执行安全检查和初始化
 */
object SecurityInitializer {
    
    private const val TAG = "SecurityInitializer"
    private var isInitialized = false
    
    /**
     * 初始化安全系统
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.w(TAG, "安全系统已经初始化")
            return
        }
        
        try {
            Log.i(TAG, "开始初始化安全系统...")
            
            // 1. 初始化SecurityManager
            SecurityManager.initialize(context)
            Log.d(TAG, "✓ SecurityManager 初始化完成")
            
            // 2. 验证配置
            val configWarnings = SecurityConfig.validateConfig()
            if (configWarnings.isNotEmpty()) {
                Log.w(TAG, "配置警告:")
                configWarnings.forEach { warning ->
                    Log.w(TAG, "  - $warning")
                }
            }
            
            // 3. 打印配置摘要
            if (SecurityConfig.ENABLE_SECURITY_LOG) {
                Log.i(TAG, SecurityConfig.getConfigSummary())
            }
            
            // 4. Root检测（可选）
            if (SecurityConfig.ENABLE_ROOT_DETECTION) {
                val isRooted = detectRoot()
                if (isRooted) {
                    handleRootDetection(context)
                }
            }
            
            // 5. 模拟器检测（可选）
            if (SecurityConfig.ENABLE_EMULATOR_DETECTION) {
                val isEmulator = detectEmulator()
                if (isEmulator) {
                    handleEmulatorDetection(context)
                }
            }
            
            isInitialized = true
            Log.i(TAG, "✓ 安全系统初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "安全系统初始化失败", e)
            // 不抛出异常，避免影响应用启动
        }
    }
    
    /**
     * 检测设备是否Root
     */
    private fun detectRoot(): Boolean {
        // 简单的Root检测（可以使用第三方库如RootBeer进行更全面的检测）
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        
        return rootPaths.any { path ->
            try {
                java.io.File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 处理Root检测结果
     */
    private fun handleRootDetection(context: Context) {
        when (SecurityConfig.ROOT_DETECTION_MODE) {
            SecurityConfig.DetectionMode.BLOCK -> {
                Log.e(TAG, "检测到Root设备，阻止应用运行")
                // 可以显示对话框并退出应用
                // 注意：这里只是记录日志，实际实现需要在Activity中处理
            }
            SecurityConfig.DetectionMode.WARNING -> {
                Log.w(TAG, "检测到Root设备，显示警告")
                // 可以显示警告对话框
            }
            SecurityConfig.DetectionMode.SILENT -> {
                Log.i(TAG, "检测到Root设备（静默模式）")
                // 只记录日志，不影响使用
            }
        }
    }
    
    /**
     * 检测是否为模拟器
     */
    private fun detectEmulator(): Boolean {
        // 简单的模拟器检测
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val model = android.os.Build.MODEL
        val product = android.os.Build.PRODUCT
        
        return (brand.startsWith("generic") && device.startsWith("generic")) ||
                model.contains("google_sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK") ||
                product.contains("sdk") ||
                product.contains("emulator")
    }
    
    /**
     * 处理模拟器检测结果
     */
    private fun handleEmulatorDetection(context: Context) {
        when (SecurityConfig.EMULATOR_DETECTION_MODE) {
            SecurityConfig.DetectionMode.BLOCK -> {
                Log.e(TAG, "检测到模拟器环境，阻止应用运行")
            }
            SecurityConfig.DetectionMode.WARNING -> {
                Log.w(TAG, "检测到模拟器环境，显示警告")
            }
            SecurityConfig.DetectionMode.SILENT -> {
                Log.i(TAG, "检测到模拟器环境（静默模式）")
            }
        }
    }
    
    /**
     * 检查安全系统是否已初始化
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * 执行安全自检
     */
    fun performSecurityCheck(context: Context): SecurityCheckResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // 1. 检查配置
            val configWarnings = SecurityConfig.validateConfig()
            warnings.addAll(configWarnings)
            
            // 2. 检查设备指纹
            try {
                val fingerprint = SecurityManager.getDeviceFingerprint(context)
                if (fingerprint.isEmpty()) {
                    issues.add("无法获取设备指纹")
                }
            } catch (e: Exception) {
                issues.add("设备指纹获取失败: ${e.message}")
            }
            
            // 3. 检查加密功能
            try {
                val testData = "test"
                val salt = SecurityManager.generateSalt()
                val encrypted = SecurityManager.encryptAES(testData, "password", salt)
                val decrypted = SecurityManager.decryptAES(encrypted, "password", salt)
                
                if (decrypted != testData) {
                    issues.add("加密/解密功能异常")
                }
            } catch (e: Exception) {
                issues.add("加密功能测试失败: ${e.message}")
            }
            
            // 4. 检查哈希功能
            try {
                val hash = SecurityManager.sha512Hash("test")
                if (hash.isEmpty()) {
                    issues.add("哈希功能异常")
                }
            } catch (e: Exception) {
                issues.add("哈希功能测试失败: ${e.message}")
            }
            
        } catch (e: Exception) {
            issues.add("安全自检失败: ${e.message}")
        }
        
        return SecurityCheckResult(
            isHealthy = issues.isEmpty(),
            issues = issues,
            warnings = warnings
        )
    }
}

/**
 * 安全检查结果
 */
data class SecurityCheckResult(
    val isHealthy: Boolean,
    val issues: List<String>,
    val warnings: List<String>
) {
    fun getSummary(): String {
        return buildString {
            appendLine("=== 安全检查结果 ===")
            appendLine("状态: ${if (isHealthy) "✓ 健康" else "✗ 异常"}")
            
            if (issues.isNotEmpty()) {
                appendLine("\n问题 (${issues.size}):")
                issues.forEach { issue ->
                    appendLine("  ✗ $issue")
                }
            }
            
            if (warnings.isNotEmpty()) {
                appendLine("\n警告 (${warnings.size}):")
                warnings.forEach { warning ->
                    appendLine("  ⚠ $warning")
                }
            }
            
            if (isHealthy && warnings.isEmpty()) {
                appendLine("\n所有安全检查通过！")
            }
            
            appendLine("==================")
        }
    }
}
