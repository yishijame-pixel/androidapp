// ValidationUtils.kt - 输入验证工具类
package com.example.funlife.utils

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

object ValidationUtils {
    
    // 用户名验证（企业级宽松规则）
    // 允许：中文、英文字母、数字、下划线、连字符、点
    // 限制：长度 2~20；首尾不能是 . - _；不能连续两个特殊字符；首字符不能是纯数字
    fun validateUsername(username: String): ValidationResult {
        val u = username.trim()
        if (u.isBlank()) return ValidationResult.Error("用户名不能为空")
        if (u.length < 2) return ValidationResult.Error("用户名至少 2 个字符")
        if (u.length > 20) return ValidationResult.Error("用户名最多 20 个字符")
        // 逐字符判断，跨平台稳定（Android 正则不支持 (?U) 内联标志）
        // 允许：Unicode 字母（含中日韩） / Unicode 数字 / _ - .
        for (ch in u) {
            val ok = ch.isLetter() || ch.isDigit() || ch == '_' || ch == '-' || ch == '.'
            if (!ok) return ValidationResult.Error("用户名只能包含中英文、数字、_ - .")
        }
        if (u[0].isDigit()) return ValidationResult.Error("用户名不能以数字开头")
        val first = u.first(); val last = u.last()
        if (first == '.' || first == '-' || first == '_' || last == '.' || last == '-' || last == '_') {
            return ValidationResult.Error("用户名首尾不能为 . - _")
        }
        // 检查连续的特殊字符（.. -- __ -. ._ 等）
        for (i in 0 until u.length - 1) {
            val a = u[i]; val b = u[i + 1]
            val aSpecial = (a == '.' || a == '-' || a == '_')
            val bSpecial = (b == '.' || b == '-' || b == '_')
            if (aSpecial && bSpecial) {
                return ValidationResult.Error("不能包含连续的 . - _")
            }
        }
        return ValidationResult.Success
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
