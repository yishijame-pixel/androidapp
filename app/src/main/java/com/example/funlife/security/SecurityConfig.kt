// SecurityConfig.kt - 安全配置中心
package com.example.funlife.security

/**
 * 安全配置中心
 * 
 * 集中管理所有安全相关的配置参数
 */
object SecurityConfig {
    
    // ==================== 加密配置 ====================
    
    /**
     * PBKDF2 迭代次数
     * 建议：10,000 - 100,000（越高越安全，但性能越慢）
     */
    const val PBKDF2_ITERATIONS = 10000
    
    /**
     * AES 密钥长度（位）
     */
    const val AES_KEY_SIZE = 256
    
    /**
     * GCM 标签长度（位）
     */
    const val GCM_TAG_LENGTH = 128
    
    /**
     * 盐值长度（字节）
     */
    const val SALT_LENGTH = 32
    
    // ==================== 会话配置 ====================
    
    /**
     * 会话有效期（天）
     */
    const val SESSION_VALIDITY_DAYS = 7L
    
    /**
     * 会话有效期（毫秒）
     */
    const val SESSION_VALIDITY_MILLIS = SESSION_VALIDITY_DAYS * 24 * 60 * 60 * 1000
    
    /**
     * 是否启用会话自动刷新
     */
    const val ENABLE_SESSION_AUTO_REFRESH = true
    
    // ==================== 金币系统配置 ====================
    
    /**
     * 每分钟最大金币操作次数
     */
    const val MAX_COIN_OPERATIONS_PER_MINUTE = 30
    
    /**
     * 可疑金额阈值（单次操作）
     */
    const val SUSPICIOUS_COIN_AMOUNT_THRESHOLD = 10000
    
    /**
     * 可疑频率阈值（每分钟操作次数）
     */
    const val SUSPICIOUS_COIN_FREQUENCY_THRESHOLD = 50
    
    /**
     * 是否启用金币操作日志
     */
    const val ENABLE_COIN_OPERATION_LOG = true
    
    /**
     * 是否启用金币异常检测
     */
    const val ENABLE_COIN_ANOMALY_DETECTION = true
    
    // ==================== VIP系统配置 ====================
    
    /**
     * 每日金币领取间隔（小时）
     */
    const val DAILY_COIN_CLAIM_INTERVAL_HOURS = 24L
    
    /**
     * 是否启用VIP状态签名验证
     */
    const val ENABLE_VIP_SIGNATURE_VERIFICATION = true
    
    /**
     * 是否启用兑换码设备绑定
     */
    const val ENABLE_REDEEM_CODE_DEVICE_BINDING = true
    
    /**
     * 兑换码哈希迭代次数
     */
    const val REDEEM_CODE_HASH_ITERATIONS = 10000
    
    // ==================== 设备指纹配置 ====================
    
    /**
     * 是否启用设备指纹验证
     */
    const val ENABLE_DEVICE_FINGERPRINT = true
    
    /**
     * 设备指纹不匹配时的处理方式
     * - STRICT: 严格模式，直接拒绝操作
     * - WARNING: 警告模式，记录日志但允许操作
     * - DISABLED: 禁用模式，不进行验证
     */
    enum class DeviceFingerprintMode {
        STRICT,
        WARNING,
        DISABLED
    }
    
    /**
     * 设备指纹验证模式
     */
    val DEVICE_FINGERPRINT_MODE = DeviceFingerprintMode.STRICT
    
    // ==================== 异常检测配置 ====================
    
    /**
     * 是否启用异常行为检测
     */
    const val ENABLE_ANOMALY_DETECTION = true
    
    /**
     * 异常行为检测灵敏度
     * - HIGH: 高灵敏度（更容易触发）
     * - MEDIUM: 中等灵敏度
     * - LOW: 低灵敏度（不容易触发）
     */
    enum class AnomalyDetectionSensitivity {
        HIGH,
        MEDIUM,
        LOW
    }
    
    /**
     * 异常检测灵敏度
     */
    val ANOMALY_DETECTION_SENSITIVITY = AnomalyDetectionSensitivity.MEDIUM
    
    /**
     * 异常行为自动封禁
     */
    const val ENABLE_AUTO_BAN = false  // 暂时禁用，避免误封
    
    /**
     * 自动封禁阈值（异常次数）
     */
    const val AUTO_BAN_THRESHOLD = 5
    
    // ==================== 调试配置 ====================
    
    /**
     * 是否启用安全日志
     * 注意：生产环境应设置为 false
     */
    const val ENABLE_SECURITY_LOG = true  // 开发阶段启用，发布时改为false
    
    /**
     * 是否启用详细日志
     */
    const val ENABLE_VERBOSE_LOG = false
    
    /**
     * 是否在调试模式下跳过某些安全检查
     * 注意：仅用于开发调试，生产环境必须为 false
     */
    const val SKIP_SECURITY_IN_DEBUG = false
    
    // ==================== Root/模拟器检测配置 ====================
    
    /**
     * 是否启用Root检测
     */
    const val ENABLE_ROOT_DETECTION = false  // 可选功能，暂时禁用
    
    /**
     * 是否启用模拟器检测
     */
    const val ENABLE_EMULATOR_DETECTION = false  // 可选功能，暂时禁用
    
    /**
     * Root/模拟器检测时的处理方式
     * - BLOCK: 阻止应用运行
     * - WARNING: 显示警告但允许运行
     * - SILENT: 静默记录但不影响使用
     */
    enum class DetectionMode {
        BLOCK,
        WARNING,
        SILENT
    }
    
    /**
     * Root检测模式
     */
    val ROOT_DETECTION_MODE = DetectionMode.WARNING
    
    /**
     * 模拟器检测模式
     */
    val EMULATOR_DETECTION_MODE = DetectionMode.SILENT
    
    // ==================== 数据库安全配置 ====================
    
    /**
     * 是否启用数据库加密（SQLCipher）
     * 注意：需要额外依赖，暂时禁用
     */
    const val ENABLE_DATABASE_ENCRYPTION = false
    
    /**
     * 数据库加密密钥长度
     */
    const val DATABASE_KEY_LENGTH = 256
    
    // ==================== 网络安全配置（未来使用） ====================
    
    /**
     * 是否启用证书固定
     */
    const val ENABLE_CERTIFICATE_PINNING = false
    
    /**
     * API请求超时时间（秒）
     */
    const val API_TIMEOUT_SECONDS = 30
    
    /**
     * 是否启用API请求签名
     */
    const val ENABLE_API_SIGNATURE = false
    
    // ==================== 工具方法 ====================
    
    /**
     * 检查是否为调试模式
     */
    fun isDebugMode(): Boolean {
        return false  // 默认为生产模式，可以通过其他方式判断
    }
    
    /**
     * 获取安全配置摘要
     */
    fun getConfigSummary(): String {
        return """
            |=== 安全配置摘要 ===
            |PBKDF2迭代次数: $PBKDF2_ITERATIONS
            |AES密钥长度: $AES_KEY_SIZE bit
            |会话有效期: $SESSION_VALIDITY_DAYS 天
            |金币操作限制: $MAX_COIN_OPERATIONS_PER_MINUTE 次/分钟
            |设备指纹验证: ${if (ENABLE_DEVICE_FINGERPRINT) "启用" else "禁用"}
            |异常检测: ${if (ENABLE_ANOMALY_DETECTION) "启用" else "禁用"}
            |Root检测: ${if (ENABLE_ROOT_DETECTION) "启用" else "禁用"}
            |调试模式: ${if (isDebugMode()) "是" else "否"}
            |==================
        """.trimMargin()
    }
    
    /**
     * 验证配置合法性
     */
    fun validateConfig(): List<String> {
        val warnings = mutableListOf<String>()
        
        if (PBKDF2_ITERATIONS < 10000) {
            warnings.add("PBKDF2迭代次数过低，建议至少10,000次")
        }
        
        if (SESSION_VALIDITY_DAYS > 30) {
            warnings.add("会话有效期过长，建议不超过30天")
        }
        
        if (ENABLE_SECURITY_LOG && !isDebugMode()) {
            warnings.add("生产环境不应启用安全日志")
        }
        
        if (SKIP_SECURITY_IN_DEBUG && !isDebugMode()) {
            warnings.add("生产环境不应跳过安全检查")
        }
        
        return warnings
    }
}
