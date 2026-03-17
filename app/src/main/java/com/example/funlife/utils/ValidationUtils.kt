// ValidationUtils.kt - 输入验证工具类
package com.example.funlife.utils

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

object ValidationUtils {
    
    // 用户名验证
    fun validateUsername(username: String): ValidationResult {
        // 先去除首尾空格
        val trimmedUsername = username.trim()
        
        return when {
            trimmedUsername.isBlank() -> ValidationResult.Error("用户名不能为空")
            trimmedUsername.length < 3 -> ValidationResult.Error("用户名至少3个字符")
            trimmedUsername.length > 20 -> ValidationResult.Error("用户名最多20个字符")
            !trimmedUsername.matches(Regex("^[a-zA-Z0-9_]+$")) -> 
                ValidationResult.Error("用户名只能包含字母、数字和下划线")
            else -> ValidationResult.Success
        }
    }
    
    // 密码验证
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("密码不能为空")
            password.length < 6 -> ValidationResult.Error("密码至少6个字符")
            password.length > 32 -> ValidationResult.Error("密码最多32个字符")
            else -> ValidationResult.Success
        }
    }
    
    // 昵称验证
    fun validateNickname(nickname: String): ValidationResult {
        return when {
            nickname.length > 20 -> ValidationResult.Error("昵称最多20个字符")
            else -> ValidationResult.Success
        }
    }
    
    // 习惯名称验证
    fun validateHabitName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("习惯名称不能为空")
            name.length > 30 -> ValidationResult.Error("习惯名称最多30个字符")
            else -> ValidationResult.Success
        }
    }
    
    // 目标标题验证
    fun validateGoalTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Error("目标标题不能为空")
            title.length > 50 -> ValidationResult.Error("目标标题最多50个字符")
            else -> ValidationResult.Success
        }
    }
    
    // 心情笔记验证
    fun validateMoodNote(note: String): ValidationResult {
        return when {
            note.length > 500 -> ValidationResult.Error("笔记最多500个字符")
            else -> ValidationResult.Success
        }
    }
    
    // 纪念日名称验证
    fun validateAnniversaryName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("纪念日名称不能为空")
            name.length > 30 -> ValidationResult.Error("纪念日名称最多30个字符")
            else -> ValidationResult.Success
        }
    }
    
    // 金币数量验证
    fun validateCoins(amount: Int): ValidationResult {
        return when {
            amount < 0 -> ValidationResult.Error("金币数量不能为负数")
            amount > 1000000 -> ValidationResult.Error("金币数量超出限制")
            else -> ValidationResult.Success
        }
    }
    
    // 转盘选项验证
    fun validateWheelOption(option: String): ValidationResult {
        return when {
            option.isBlank() -> ValidationResult.Error("选项不能为空")
            option.length > 20 -> ValidationResult.Error("选项最多20个字符")
            else -> ValidationResult.Success
        }
    }
    
    // 权重验证
    fun validateWeight(weight: Int): ValidationResult {
        return when {
            weight < 1 -> ValidationResult.Error("权重至少为1")
            weight > 100 -> ValidationResult.Error("权重最多为100")
            else -> ValidationResult.Success
        }
    }
    
    // 日期格式验证
    fun validateDateFormat(dateStr: String): ValidationResult {
        return try {
            java.time.LocalDate.parse(dateStr)
            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error("日期格式错误，应为 yyyy-MM-dd")
        }
    }
    
    // 邮箱验证（如果需要）
    fun validateEmail(email: String): ValidationResult {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            email.isBlank() -> ValidationResult.Error("邮箱不能为空")
            !email.matches(emailRegex) -> ValidationResult.Error("邮箱格式不正确")
            else -> ValidationResult.Success
        }
    }
}
