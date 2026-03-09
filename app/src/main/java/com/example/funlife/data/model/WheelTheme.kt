// WheelTheme.kt - 转盘主题数据模型
package com.example.funlife.data.model

import androidx.compose.ui.graphics.Color

/**
 * 转盘主题
 */
data class WheelTheme(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val wheelColors: List<Pair<Color, Color>>, // 转盘扇形颜色对
    val isSeasonalTheme: Boolean = false,
    val isDefault: Boolean = false
)

/**
 * 预设主题
 */
object WheelThemes {
    
    // 默认主题
    val Default = WheelTheme(
        id = "default",
        name = "默认",
        emoji = "🎨",
        description = "经典配色方案",
        primaryColor = Color(0xFF6366F1),
        secondaryColor = Color(0xFF8B5CF6),
        accentColor = Color(0xFFEC4899),
        backgroundColor = Color(0xFFF5F5F5),
        wheelColors = listOf(
            Pair(Color(0xFFFF6B9D), Color(0xFFC44569)),
            Pair(Color(0xFF4FACFE), Color(0xFF00F2FE)),
            Pair(Color(0xFFFFA726), Color(0xFFFB8C00)),
            Pair(Color(0xFF66BB6A), Color(0xFF43A047)),
            Pair(Color(0xFFAB47BC), Color(0xFF8E24AA)),
            Pair(Color(0xFFFFCA28), Color(0xFFFFA000)),
            Pair(Color(0xFF26C6DA), Color(0xFF00ACC1)),
            Pair(Color(0xFFEF5350), Color(0xFFE53935))
        ),
        isDefault = true
    )
    
    // 春节主题
    val SpringFestival = WheelTheme(
        id = "spring_festival",
        name = "春节",
        emoji = "🧧",
        description = "喜庆的春节配色",
        primaryColor = Color(0xFFFF0000),
        secondaryColor = Color(0xFFFFD700),
        accentColor = Color(0xFFFF6B6B),
        backgroundColor = Color(0xFFFFF5F5),
        wheelColors = listOf(
            Pair(Color(0xFFFF0000), Color(0xFFCC0000)),
            Pair(Color(0xFFFFD700), Color(0xFFFFA500)),
            Pair(Color(0xFFFF6B6B), Color(0xFFFF4444)),
            Pair(Color(0xFFFFE4B5), Color(0xFFFFD700)),
            Pair(Color(0xFFFF8C00), Color(0xFFFF6347)),
            Pair(Color(0xFFDC143C), Color(0xFFB22222)),
            Pair(Color(0xFFFFB6C1), Color(0xFFFF69B4)),
            Pair(Color(0xFFFFA07A), Color(0xFFFF7F50))
        ),
        isSeasonalTheme = true
    )
    
    // 圣诞主题
    val Christmas = WheelTheme(
        id = "christmas",
        name = "圣诞节",
        emoji = "🎄",
        description = "温馨的圣诞配色",
        primaryColor = Color(0xFF228B22),
        secondaryColor = Color(0xFFDC143C),
        accentColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFFF0FFF0),
        wheelColors = listOf(
            Pair(Color(0xFF228B22), Color(0xFF006400)),
            Pair(Color(0xFFDC143C), Color(0xFFB22222)),
            Pair(Color(0xFFFFFFFF), Color(0xFFF0F0F0)),
            Pair(Color(0xFFFFD700), Color(0xFFFFA500)),
            Pair(Color(0xFF32CD32), Color(0xFF228B22)),
            Pair(Color(0xFFFF6347), Color(0xFFFF4500)),
            Pair(Color(0xFFF0E68C), Color(0xFFEEE8AA)),
            Pair(Color(0xFF8B4513), Color(0xFFA0522D))
        ),
        isSeasonalTheme = true
    )
    
    // 夜间模式
    val Dark = WheelTheme(
        id = "dark",
        name = "夜间",
        emoji = "🌙",
        description = "护眼的深色主题",
        primaryColor = Color(0xFF6366F1),
        secondaryColor = Color(0xFF8B5CF6),
        accentColor = Color(0xFFEC4899),
        backgroundColor = Color(0xFF1A1A1A),
        wheelColors = listOf(
            Pair(Color(0xFF667EEA), Color(0xFF764BA2)),
            Pair(Color(0xFF6366F1), Color(0xFF4F46E5)),
            Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
            Pair(Color(0xFF06B6D4), Color(0xFF0891B2)),
            Pair(Color(0xFF3B82F6), Color(0xFF2563EB)),
            Pair(Color(0xFF10B981), Color(0xFF059669)),
            Pair(Color(0xFFEC4899), Color(0xFFDB2777)),
            Pair(Color(0xFFF59E0B), Color(0xFFD97706))
        )
    )
    
    // 彩虹主题
    val Rainbow = WheelTheme(
        id = "rainbow",
        name = "彩虹",
        emoji = "🌈",
        description = "绚丽的彩虹配色",
        primaryColor = Color(0xFFFF0080),
        secondaryColor = Color(0xFF00FFFF),
        accentColor = Color(0xFFFFFF00),
        backgroundColor = Color(0xFFFFFAFA),
        wheelColors = listOf(
            Pair(Color(0xFFFF0000), Color(0xFFFF4444)),
            Pair(Color(0xFFFF7F00), Color(0xFFFFAA44)),
            Pair(Color(0xFFFFFF00), Color(0xFFFFFF44)),
            Pair(Color(0xFF00FF00), Color(0xFF44FF44)),
            Pair(Color(0xFF0000FF), Color(0xFF4444FF)),
            Pair(Color(0xFF4B0082), Color(0xFF6A44AA)),
            Pair(Color(0xFF9400D3), Color(0xFFAA44DD)),
            Pair(Color(0xFFFF1493), Color(0xFFFF44AA))
        )
    )
    
    // 海洋主题
    val Ocean = WheelTheme(
        id = "ocean",
        name = "海洋",
        emoji = "🌊",
        description = "清新的海洋配色",
        primaryColor = Color(0xFF0077BE),
        secondaryColor = Color(0xFF00CED1),
        accentColor = Color(0xFF40E0D0),
        backgroundColor = Color(0xFFE0F7FA),
        wheelColors = listOf(
            Pair(Color(0xFF0077BE), Color(0xFF005A8C)),
            Pair(Color(0xFF00CED1), Color(0xFF00A8AA)),
            Pair(Color(0xFF40E0D0), Color(0xFF20C0B0)),
            Pair(Color(0xFF5F9EA0), Color(0xFF4A7A7C)),
            Pair(Color(0xFF4682B4), Color(0xFF36648B)),
            Pair(Color(0xFF87CEEB), Color(0xFF6AACCC)),
            Pair(Color(0xFF00BFFF), Color(0xFF009FDD)),
            Pair(Color(0xFF1E90FF), Color(0xFF1870CC))
        )
    )
    
    // 森林主题
    val Forest = WheelTheme(
        id = "forest",
        name = "森林",
        emoji = "🌲",
        description = "自然的森林配色",
        primaryColor = Color(0xFF228B22),
        secondaryColor = Color(0xFF32CD32),
        accentColor = Color(0xFF90EE90),
        backgroundColor = Color(0xFFF0FFF0),
        wheelColors = listOf(
            Pair(Color(0xFF228B22), Color(0xFF1A6B1A)),
            Pair(Color(0xFF32CD32), Color(0xFF28A828)),
            Pair(Color(0xFF90EE90), Color(0xFF70CE70)),
            Pair(Color(0xFF3CB371), Color(0xFF2E8B57)),
            Pair(Color(0xFF2E8B57), Color(0xFF246B45)),
            Pair(Color(0xFF66CDAA), Color(0xFF4CAA88)),
            Pair(Color(0xFF8FBC8F), Color(0xFF6F9C6F)),
            Pair(Color(0xFF556B2F), Color(0xFF3D4F21))
        )
    )
    
    // 获取所有主题
    fun getAllThemes(): List<WheelTheme> {
        return listOf(
            Default,
            SpringFestival,
            Christmas,
            Dark,
            Rainbow,
            Ocean,
            Forest
        )
    }
    
    // 根据ID获取主题
    fun getThemeById(id: String): WheelTheme {
        return getAllThemes().find { it.id == id } ?: Default
    }
    
    // 获取季节性主题
    fun getSeasonalThemes(): List<WheelTheme> {
        return getAllThemes().filter { it.isSeasonalTheme }
    }
}
