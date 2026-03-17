// PasswordHasher.kt - 密码哈希工具类
package com.example.funlife.utils

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 密码哈希工具类
 * 使用 PBKDF2 算法进行密码哈希
 */
object PasswordHasher {
    
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    
    /**
     * 哈希密码
     * @param password 明文密码
     * @return 哈希后的密码（格式：salt:hash）
     */
    fun hashPassword(password: String): String {
        // 生成随机盐值
        val salt = generateSalt()
        
        // 使用 PBKDF2 进行哈希
        val hash = pbkdf2(password, salt)
        
        // 返回格式：salt:hash（都转换为十六进制字符串）
        return "${bytesToHex(salt)}:${bytesToHex(hash)}"
    }
    
    /**
     * 验证密码
     * @param password 明文密码
     * @param hashedPassword 哈希后的密码（格式：salt:hash）
     * @return 是否匹配
     */
    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        try {
            // 分离盐值和哈希值
            val parts = hashedPassword.split(":")
            if (parts.size != 2) {
                return false
            }
            
            val salt = hexToBytes(parts[0])
            val originalHash = hexToBytes(parts[1])
            
            // 使用相同的盐值对输入密码进行哈希
            val testHash = pbkdf2(password, salt)
            
            // 比较哈希值（使用常量时间比较防止时序攻击）
            return constantTimeEquals(originalHash, testHash)
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 检查密码是否已经哈希
     * @param password 密码字符串
     * @return 是否已哈希
     */
    fun isHashed(password: String): Boolean {
        // 哈希密码的格式是 "salt:hash"，都是十六进制字符串
        val parts = password.split(":")
        if (parts.size != 2) {
            return false
        }
        
        // 检查是否都是有效的十六进制字符串
        return parts.all { part ->
            part.length % 2 == 0 && part.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        }
    }
    
    /**
     * 生成随机盐值
     */
    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }
    
    /**
     * PBKDF2 哈希算法
     */
    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 十六进制字符串转字节数组
     */
    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
    
    /**
     * 常量时间比较（防止时序攻击）
     */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) {
            return false
        }
        
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
    
    /**
     * 迁移旧密码（明文）到哈希密码
     * 用于数据库迁移
     */
    fun migrateOldPassword(oldPassword: String): String {
        // 如果已经是哈希密码，直接返回
        if (isHashed(oldPassword)) {
            return oldPassword
        }
        
        // 否则进行哈希
        return hashPassword(oldPassword)
    }
}
