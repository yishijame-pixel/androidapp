// DateUtils.kt - 日期工具类
package com.example.funlife.utils

import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    
    private const val TAG = "DateUtils"
    
    // 标准日期格式
    const val FORMAT_DATE = "yyyy-MM-dd"
    const val FORMAT_DATETIME = "yyyy-MM-dd HH:mm:ss"
    const val FORMAT_DISPLAY_DATE = "yyyy年MM月dd日"
    const val FORMAT_DISPLAY_DATETIME = "yyyy年MM月dd日 HH:mm"
    const val FORMAT_SHORT_DATE = "MM月dd日"
    const val FORMAT_SHORT_DATETIME = "MM-dd HH:mm"
    
    // 🔥 安全的日期解析
    fun parseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse date: $dateStr", e)
            null
        }
    }
    
    // 🔥 安全的日期时间解析
    fun parseDateTime(dateTimeStr: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateTimeStr)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse datetime: $dateTimeStr", e)
            null
        }
    }
    
    // 🔥 格式化日期
    fun formatDate(date: LocalDate, pattern: String = FORMAT_DATE): String {
        return try {
            date.format(DateTimeFormatter.ofPattern(pattern))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format date: $date", e)
            date.toString()
        }
    }
    
    // 🔥 格式化日期时间
    fun formatDateTime(dateTime: LocalDateTime, pattern: String = FORMAT_DATETIME): String {
        return try {
            dateTime.format(DateTimeFormatter.ofPattern(pattern))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format datetime: $dateTime", e)
            dateTime.toString()
        }
    }
    
    // 🔥 格式化日期字符串（从字符串到字符串）
    fun formatDateString(
        dateStr: String,
        inputPattern: String = FORMAT_DATE,
        outputPattern: String = FORMAT_DISPLAY_DATE
    ): String {
        return try {
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(inputPattern))
            date.format(DateTimeFormatter.ofPattern(outputPattern))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format date string: $dateStr", e)
            dateStr
        }
    }
    
    // 🔥 获取当前日期字符串
    fun getCurrentDateString(pattern: String = FORMAT_DATE): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern))
    }
    
    // 🔥 获取当前日期时间字符串
    fun getCurrentDateTimeString(pattern: String = FORMAT_DATETIME): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))
    }
    
    // 🔥 计算两个日期之间的天数
    fun daysBetween(startDate: String, endDate: String): Long? {
        return try {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            ChronoUnit.DAYS.between(start, end)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to calculate days between: $startDate and $endDate", e)
            null
        }
    }
    
    // 🔥 计算距离今天的天数（正数表示未来，负数表示过去）
    fun daysFromToday(dateStr: String): Long? {
        return try {
            val date = LocalDate.parse(dateStr)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, date)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to calculate days from today: $dateStr", e)
            null
        }
    }
    
    // 🔥 检查日期是否是今天
    fun isToday(dateStr: String): Boolean {
        return try {
            val date = LocalDate.parse(dateStr)
            date == LocalDate.now()
        } catch (e: Exception) {
            false
        }
    }
    
    // 🔥 检查日期是否在过去
    fun isPast(dateStr: String): Boolean {
        return try {
            val date = LocalDate.parse(dateStr)
            date.isBefore(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }
    
    // 🔥 检查日期是否在未来
    fun isFuture(dateStr: String): Boolean {
        return try {
            val date = LocalDate.parse(dateStr)
            date.isAfter(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }
    
    // 🔥 获取友好的时间描述
    fun getFriendlyTimeDescription(dateStr: String): String {
        val days = daysFromToday(dateStr) ?: return dateStr
        
        return when {
            days == 0L -> "今天"
            days == 1L -> "明天"
            days == -1L -> "昨天"
            days > 0 && days <= 7 -> "还有${days}天"
            days > 7 && days <= 30 -> "还有${days}天"
            days > 30 -> {
                val months = days / 30
                "还有约${months}个月"
            }
            days < 0 && days >= -7 -> "已过去${-days}天"
            days < -7 && days >= -30 -> "已过去${-days}天"
            days < -30 -> {
                val months = -days / 30
                "已过去约${months}个月"
            }
            else -> dateStr
        }
    }
    
    // 🔥 获取星期几
    fun getDayOfWeek(dateStr: String): String? {
        return try {
            val date = LocalDate.parse(dateStr)
            val dayOfWeek = date.dayOfWeek
            when (dayOfWeek.value) {
                1 -> "星期一"
                2 -> "星期二"
                3 -> "星期三"
                4 -> "星期四"
                5 -> "星期五"
                6 -> "星期六"
                7 -> "星期日"
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get day of week: $dateStr", e)
            null
        }
    }
    
    // 🔥 验证日期格式
    fun isValidDate(dateStr: String, pattern: String = FORMAT_DATE): Boolean {
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // 🔥 获取日期范围内的所有日期
    fun getDateRange(startDate: String, endDate: String): List<String> {
        return try {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            val dates = mutableListOf<String>()
            var current = start
            
            while (!current.isAfter(end)) {
                dates.add(current.toString())
                current = current.plusDays(1)
            }
            
            dates
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get date range: $startDate to $endDate", e)
            emptyList()
        }
    }
    
    // 🔥 获取本周的日期范围
    fun getThisWeekRange(): Pair<String, String> {
        val today = LocalDate.now()
        val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val sunday = monday.plusDays(6)
        return Pair(monday.toString(), sunday.toString())
    }
    
    // 🔥 获取本月的日期范围
    fun getThisMonthRange(): Pair<String, String> {
        val today = LocalDate.now()
        val firstDay = today.withDayOfMonth(1)
        val lastDay = today.withDayOfMonth(today.lengthOfMonth())
        return Pair(firstDay.toString(), lastDay.toString())
    }
}
