// SecurityManager.kt - 安全管理器（军事级加密系统）
package com.example.funlife.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * 军事级安全管理器
 * 
 * 特性：
 * - AES-256-GCM 加密（军事级别）
 * - SHA-512 哈希
 * - 设备指纹绑定
 * - 时间戳防篡改
 * - 随机盐值生成
 * - 三重加密：AES + XOR + 自定义Base64
 * - PBKDF2 密钥派生（10,000次迭代）
 * - Android Keystore 集成（API 23+）
 */
object SecurityManager {
    
    private const val KEYSTORE_ALIAS = "FunLifeSecureKey"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_SIZE = 256
    
    // 混淆密钥（多层加密）
    private val OBFUSCATION_KEYS = listOf(
        "FL2024SecureKey#1@VIP",
        "FunLife#Crypto$2024",
        "VIP@Secure#System!2024"
    )
    
    /**
     * 初始化安全系统
     */
    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initializeKeystore()
        }
    }
    
    /**
     * 初始化 Android Keystore
     */
    private fun initializeKeystore() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                
                if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        "AndroidKeyStore"
                    )
                    
                    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_SIZE)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                    
                    keyGenerator.init(keyGenParameterSpec)
                    keyGenerator.generateKey()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 获取设备指纹（唯一标识）
     */
    fun getDeviceFingerprint(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        
        val deviceInfo = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.DEVICE}_$androidId"
        return sha512Hash(deviceInfo)
    }
    
    /**
     * SHA-512 哈希
     */
    fun sha512Hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 生成随机盐值
     */
    fun generateSalt(length: Int = 32): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * PBKDF2 密钥派生（10,000次迭代）
     */
    private fun deriveKey(password: String, salt: String): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(
            password.toCharArray(),
            salt.toByteArray(StandardCharsets.UTF_8),
            PBKDF2_ITERATIONS,
            KEY_SIZE
        )
        return factory.generateSecret(spec).encoded
    }
    
    /**
     * AES-256-GCM 加密
     */
    fun encryptAES(plaintext: String, password: String, salt: String): String {
        try {
            val keyBytes = deriveKey(password, salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            
            // 组合: IV + 加密数据
            val combined = iv + encryptedBytes
            return customBase64Encode(combined)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
    
    /**
     * AES-256-GCM 解密
     */
    fun decryptAES(ciphertext: String, password: String, salt: String): String {
        try {
            val combined = customBase64Decode(ciphertext)
            
            // 分离 IV 和加密数据
            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)
            
            val keyBytes = deriveKey(password, salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
    
    /**
     * 三重加密（AES + XOR + 自定义Base64）
     */
    fun tripleEncrypt(plaintext: String, context: Context): String {
        var encrypted = plaintext
        
        // 第一层：AES-256-GCM
        val salt1 = generateSalt()
        encrypted = encryptAES(encrypted, OBFUSCATION_KEYS[0], salt1)
        
        // 第二层：XOR 混淆
        encrypted = xorEncrypt(encrypted, OBFUSCATION_KEYS[1])
        
        // 第三层：设备指纹绑定
        val deviceFingerprint = getDeviceFingerprint(context)
        encrypted = xorEncrypt(encrypted, deviceFingerprint.substring(0, 32))
        
        // 添加盐值标记
        return "$salt1:$encrypted"
    }
    
    /**
     * 三重解密
     */
    fun tripleDecrypt(ciphertext: String, context: Context): String {
        try {
            val parts = ciphertext.split(":")
            if (parts.size != 2) return ""
            
            val salt1 = parts[0]
            var encrypted = parts[1]
            
            // 第三层：设备指纹解绑
            val deviceFingerprint = getDeviceFingerprint(context)
            encrypted = xorDecrypt(encrypted, deviceFingerprint.substring(0, 32))
            
            // 第二层：XOR 解密
            encrypted = xorDecrypt(encrypted, OBFUSCATION_KEYS[1])
            
            // 第一层：AES-256-GCM 解密
            encrypted = decryptAES(encrypted, OBFUSCATION_KEYS[0], salt1)
            
            return encrypted
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
    
    /**
     * XOR 加密/解密
     */
    private fun xorEncrypt(input: String, key: String): String {
        val inputBytes = input.toByteArray(StandardCharsets.UTF_8)
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        val result = ByteArray(inputBytes.size)
        
        for (i in inputBytes.indices) {
            result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        
        return customBase64Encode(result)
    }
    
    private fun xorDecrypt(input: String, key: String): String {
        val inputBytes = customBase64Decode(input)
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        val result = ByteArray(inputBytes.size)
        
        for (i in inputBytes.indices) {
            result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        
        return String(result, StandardCharsets.UTF_8)
    }
    
    /**
     * 自定义 Base64 编码（增加混淆）
     */
    private fun customBase64Encode(input: ByteArray): String {
        val base64 = android.util.Base64.encodeToString(input, android.util.Base64.NO_WRAP)
        // 字符替换混淆
        return base64
            .replace('+', '-')
            .replace('/', '_')
            .replace('=', '.')
    }
    
    private fun customBase64Decode(input: String): ByteArray {
        // 还原字符
        val base64 = input
            .replace('-', '+')
            .replace('_', '/')
            .replace('.', '=')
        return android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
    }
    
    /**
     * 生成安全的时间戳令牌（防篡改）
     */
    fun generateSecureTimestamp(context: Context, timestamp: Long): String {
        val deviceFingerprint = getDeviceFingerprint(context)
        val data = "$timestamp:$deviceFingerprint:${OBFUSCATION_KEYS[2]}"
        val hash = sha512Hash(data)
        return "$timestamp:$hash"
    }
    
    /**
     * 验证时间戳令牌
     */
    fun verifySecureTimestamp(context: Context, token: String): Long? {
        try {
            val parts = token.split(":")
            if (parts.size != 2) return null
            
            val timestamp = parts[0].toLongOrNull() ?: return null
            val hash = parts[1]
            
            val deviceFingerprint = getDeviceFingerprint(context)
            val expectedData = "$timestamp:$deviceFingerprint:${OBFUSCATION_KEYS[2]}"
            val expectedHash = sha512Hash(expectedData)
            
            // 时间戳验证（防止篡改）
            if (hash != expectedHash) return null
            
            return timestamp
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 哈希兑换码（PBKDF2 风格，10,000次迭代 - 保持军事级安全）
     * 注意：此方法应在后台线程调用
     */
    fun hashRedeemCode(code: String, context: Context): String {
        val deviceFingerprint = getDeviceFingerprint(context)
        val salt = "${OBFUSCATION_KEYS[0]}:$deviceFingerprint"
        
        // 保持10,000次迭代，确保军事级安全
        var hash = code
        repeat(PBKDF2_ITERATIONS) {
            hash = sha512Hash("$hash:$salt:$it")
        }
        
        return hash
    }
    
    /**
     * 验证兑换码
     */
    fun verifyRedeemCode(inputCode: String, storedHash: String, context: Context): Boolean {
        val computedHash = hashRedeemCode(inputCode, context)
        return computedHash == storedHash
    }
    
    /**
     * 生成防篡改的VIP状态签名
     */
    fun generateVipSignature(
        userId: Long,
        vipLevel: Int,
        expireDate: String?,
        context: Context
    ): String {
        val deviceFingerprint = getDeviceFingerprint(context)
        val data = "$userId:$vipLevel:$expireDate:$deviceFingerprint:${OBFUSCATION_KEYS[1]}"
        return sha512Hash(data)
    }
    
    /**
     * 验证VIP状态签名
     */
    fun verifyVipSignature(
        userId: Long,
        vipLevel: Int,
        expireDate: String?,
        signature: String,
        context: Context
    ): Boolean {
        val expectedSignature = generateVipSignature(userId, vipLevel, expireDate, context)
        return signature == expectedSignature
    }
}
