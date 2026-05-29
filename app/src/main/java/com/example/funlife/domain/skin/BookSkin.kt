// ═══════════════════════════════════════════════════════════════════════════
// BookSkin.kt — 日记本皮肤 schema（domain 层，不依赖 Compose UI）
//
// 设计目标：
//   · 不可变值类型，方便 Compose 跳过相等重组
//   · 全部字段可序列化（为后续远程下发铺路）
//   · 颜色、字体、几何、材质、装饰、特效 6 个子结构独立演化
//   · 字符串走 R.string，颜色用 ULong 存（便于 JSON 化）
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.domain.skin

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// 标识
// ─────────────────────────────────────────────────────────────
@JvmInline
value class SkinId(val raw: String) {
    init {
        require(raw.contains("::")) { "SkinId must contain namespace, got: $raw" }
    }
    val namespace: String get() = raw.substringBefore("::")
    val name: String get() = raw.substringAfter("::")
    override fun toString(): String = raw

    companion object {
        fun of(namespace: String, name: String) = SkinId("$namespace::$name")
    }
}

// ─────────────────────────────────────────────────────────────
// 元信息
// ─────────────────────────────────────────────────────────────
@Immutable
data class SkinMeta(
    @StringRes val displayNameRes: Int,
    @StringRes val descriptionRes: Int,
    val author: String,
    val version: Int,
    val unlock: Unlock
)

// ─────────────────────────────────────────────────────────────
// 解锁条件（先留枚举，业务层 Gate 实现）
// ─────────────────────────────────────────────────────────────
sealed interface Unlock {
    data object Free : Unlock
    data class Vip(val level: Int) : Unlock
    data class Purchase(val sku: String) : Unlock
    data class Event(val campaignId: String) : Unlock
    data class Achievement(val achievementId: String) : Unlock
}

// ─────────────────────────────────────────────────────────────
// 颜色对（基色 + 高光/阴影），方便所有"渐变"统一表达
// ─────────────────────────────────────────────────────────────
@Immutable
data class ColorPair(val base: Color, val accent: Color)

// ─────────────────────────────────────────────────────────────
// 五金件 / 装饰类型枚举
// ─────────────────────────────────────────────────────────────
enum class OrnamentType { None, Vine, Rune, Sakura, Bamboo, Flame, Moon, Stars }
enum class RibbonStyle { None, Plain, Jewel, Petal }
enum class ClaspStyle { None, BeltBuckle, HexStar, Pearl }

// ─────────────────────────────────────────────────────────────
// 子结构 1：调色板
// ─────────────────────────────────────────────────────────────
@Immutable
data class SkinPalette(
    val cover: ColorPair,
    val coverShadow: Color,
    val spine: ColorPair,
    val paper: Color,
    val paperFiber: Color,
    val ink: Color,                  // 主文字
    val inkSoft: Color,              // 次级文字
    val foil: ColorPair,             // 烫金/烫银
    val brass: ColorPair,            // 五金
    val ribbon: Color,
    val ruling: Color,               // 横格线
    val pageEdge: Color,             // 纸张切面颜色
    val pageEdgeDark: Color,         // 切面深色分章节线
    val seal: Color                  // 朱砂印章
)

// ─────────────────────────────────────────────────────────────
// 子结构 2：字体气质
// ─────────────────────────────────────────────────────────────
@Immutable
data class SkinTypography(
    val titleFontFamily: FontFamily,
    val bodyFontFamily: FontFamily,
    val titleLetterSpacingEm: Float,
    val bodyLetterSpacingEm: Float,
    val titleEmboss: Boolean
)

// ─────────────────────────────────────────────────────────────
// 子结构 3：几何
// ─────────────────────────────────────────────────────────────
@Immutable
data class SkinGeometry(
    val coverCornerRadius: Dp = 6.dp,
    val spineWidth: Dp = 8.dp,
    val pageStackCountHigh: Int = 240,
    val pageStackCountMid: Int = 120,
    val pageStackCountLow: Int = 60,
    val pageStackLineAlphaMin: Float = 0.10f,
    val pageStackLineAlphaMax: Float = 0.35f,
    val rulingSpacing: Dp = 32.dp,
    val pagePaddingHorizontal: Dp = 28.dp,
    val pagePaddingVertical: Dp = 36.dp
)

// ─────────────────────────────────────────────────────────────
// 子结构 4：材质（噪点、纤维等）
// ─────────────────────────────────────────────────────────────
@Immutable
data class SkinMaterials(
    val leatherNoiseCount: Int = 4000,
    val leatherNoiseAlpha: Float = 0.04f,
    val paperNoiseCount: Int = 1500,
    val paperNoiseAlpha: Float = 0.06f,
    val paperFiberCount: Int = 200,
    val paperFiberAlpha: Float = 0.10f,
    val foilDoubleStroke: Boolean = true
)

// ─────────────────────────────────────────────────────────────
// 子结构 5：装饰
// ─────────────────────────────────────────────────────────────
@Immutable
data class SkinOrnaments(
    val cornerOrnament: OrnamentType = OrnamentType.Vine,
    val ribbonStyle: RibbonStyle = RibbonStyle.Plain,
    val claspStyle: ClaspStyle = ClaspStyle.BeltBuckle,
    val showRings: Boolean = true,        // 锡环（左侧装订线）
    val showSeal: Boolean = true          // 朱砂印章（封面右下）
)

// ─────────────────────────────────────────────────────────────
// 子结构 6：特效
// ─────────────────────────────────────────────────────────────
@Immutable
data class SkinEffects(
    val particlesOnOpen: Boolean = false,
    val particleCount: Int = 0,
    val breathing: Boolean = true,
    val flipHapticEnabled: Boolean = true
)

// ─────────────────────────────────────────────────────────────
// 顶层皮肤值类型
// ─────────────────────────────────────────────────────────────
@Immutable
data class BookSkin(
    val id: SkinId,
    val meta: SkinMeta,
    val palette: SkinPalette,
    val typography: SkinTypography,
    val geometry: SkinGeometry = SkinGeometry(),
    val materials: SkinMaterials = SkinMaterials(),
    val ornaments: SkinOrnaments = SkinOrnaments(),
    val effects: SkinEffects = SkinEffects()
) {
    /** 标题文案：取 R.string，调用方决定是否覆盖（用户自定义书名）。 */
    val titleResId: Int get() = meta.displayNameRes
}
