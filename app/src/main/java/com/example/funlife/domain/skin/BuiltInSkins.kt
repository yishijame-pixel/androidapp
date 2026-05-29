// ═══════════════════════════════════════════════════════════════════════════
// BuiltInSkins.kt — 内置皮肤预设
//
// P0 阶段：完整实现"蘅芜旧卷"，"霁月长明"与"晴川早春"占位 = 复用 HengWu
//   ↑ 这样 SkinRepository / Provider / 切换 UI 可以提前打通，
//     P1 时只需把这两套的 palette / typography 替换为真正的设计即可，
//     业务侧零改动。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.domain.skin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.funlife.R

object BuiltInSkins {

    /** 蘅芜旧卷 —— 默认 / 文人古籍风。 */
    val HengWu: BookSkin = BookSkin(
        id = SkinId.of("builtin", "hengwu"),
        meta = SkinMeta(
            displayNameRes = R.string.skin_hengwu_name,
            descriptionRes = R.string.skin_hengwu_desc,
            author = "FunLife",
            version = 1,
            unlock = Unlock.Free
        ),
        palette = SkinPalette(
            cover         = ColorPair(Color(0xFF1F2A3D), Color(0xFF2A3B58)),  // 藏青布面
            coverShadow   = Color(0xFF1B2638),
            spine         = ColorPair(Color(0xFF15203A), Color(0xFF1F2A3D)),
            paper         = Color(0xFFECDCAA),
            paperFiber    = Color(0xFFB89A6A),
            ink           = Color(0xFF3D2A1F),
            inkSoft       = Color(0xFF8B6F4E),
            foil          = ColorPair(Color(0xFFE8C676), Color(0xFFA07A1F)),  // 烫金
            brass         = ColorPair(Color(0xFFB89B5A), Color(0xFFFFE08A)),
            ribbon        = Color(0xFFB23A48),
            ruling        = Color(0xFF8B6F4E),
            pageEdge      = Color(0xFFD8C28B),
            pageEdgeDark  = Color(0xFF8B6F4E),
            seal          = Color(0xFFB23A48)
        ),
        typography = SkinTypography(
            titleFontFamily = FontFamily.Serif,
            bodyFontFamily  = FontFamily.Serif,
            titleLetterSpacingEm = 0.4f,
            bodyLetterSpacingEm  = 0.08f,
            titleEmboss = true
        ),
        geometry  = SkinGeometry(),
        materials = SkinMaterials(),
        ornaments = SkinOrnaments(
            cornerOrnament = OrnamentType.Vine,
            ribbonStyle    = RibbonStyle.Plain,
            claspStyle     = ClaspStyle.BeltBuckle,
            showRings      = true,
            showSeal       = true
        ),
        effects = SkinEffects(
            particlesOnOpen = false,
            particleCount   = 0,
            breathing       = true,
            flipHapticEnabled = true
        )
    )

    /** 霁月长明 —— 深空魔法书：黑曜石 + 银箔 + 月光纸。 */
    val JiYue: BookSkin = BookSkin(
        id = SkinId.of("builtin", "jiyue"),
        meta = SkinMeta(
            displayNameRes = R.string.skin_jiyue_name,
            descriptionRes = R.string.skin_jiyue_desc,
            author = "FunLife",
            version = 1,
            unlock = Unlock.Free
        ),
        palette = SkinPalette(
            cover         = ColorPair(Color(0xFF0E0A1F), Color(0xFF3D2C5C)),  // 黑紫渐变
            coverShadow   = Color(0xFF080514),
            spine         = ColorPair(Color(0xFF050310), Color(0xFF0E0A1F)),
            paper         = Color(0xFFF5EFE1),                                 // 月光纸
            paperFiber    = Color(0xFF6E4FCF),                                 // 紫雾纤维
            ink           = Color(0xFF1A1133),
            inkSoft       = Color(0xFF3D2C5C),
            foil          = ColorPair(Color(0xFFCFD3DB), Color(0xFF8C92A0)),   // 烫银
            brass         = ColorPair(Color(0xFF9DA3B0), Color(0xFFE8ECF5)),
            ribbon        = Color(0xFF6E4FCF),
            ruling        = Color(0xFF3D2C5C),
            pageEdge      = Color(0xFFCFD3DB),
            pageEdgeDark  = Color(0xFF3D2C5C),
            seal          = Color(0xFF6E4FCF)
        ),
        typography = SkinTypography(
            titleFontFamily = FontFamily.Serif,
            bodyFontFamily  = FontFamily.Serif,
            titleLetterSpacingEm = 0.5f,
            bodyLetterSpacingEm  = 0.10f,
            titleEmboss = true
        ),
        ornaments = SkinOrnaments(
            cornerOrnament = OrnamentType.Moon,
            ribbonStyle    = RibbonStyle.Jewel,
            claspStyle     = ClaspStyle.HexStar,
            showRings      = true,
            showSeal       = true
        ),
        effects = SkinEffects(
            particlesOnOpen = true,
            particleCount   = 24,
            breathing       = true,
            flipHapticEnabled = true
        )
    )

    /** 晴川早春 —— 文艺淡彩：奶油皮革 + 玫瑰金樱 + 雪白纸。 */
    val QingChuan: BookSkin = BookSkin(
        id = SkinId.of("builtin", "qingchuan"),
        meta = SkinMeta(
            displayNameRes = R.string.skin_qingchuan_name,
            descriptionRes = R.string.skin_qingchuan_desc,
            author = "FunLife",
            version = 1,
            unlock = Unlock.Free
        ),
        palette = SkinPalette(
            cover         = ColorPair(Color(0xFFF8E6D0), Color(0xFFEFD0AF)),  // 米奶皮革（略加色相变化）
            coverShadow   = Color(0xFFB89378),                                 // 加深的奶咖阴影
            spine         = ColorPair(Color(0xFFE0BFA0), Color(0xFFF5E0C8)),
            paper         = Color(0xFFFFF8F0),
            paperFiber    = Color(0xFFFFB6C1),
            ink           = Color(0xFF5A2D12),                                 // 加深棕褐
            inkSoft       = Color(0xFFB86B5C),
            // 玫瑰金 → 古铜玫瑰：跟奶油底对比度足够，文字能看清
            foil          = ColorPair(Color(0xFFB8533F), Color(0xFF8C3220)),
            brass         = ColorPair(Color(0xFFE8A088), Color(0xFFFFFFFF)),
            ribbon        = Color(0xFFE89BAA),                                 // 樱花粉丝带（略加深）
            ruling        = Color(0xFFB8533F),
            pageEdge      = Color(0xFFFFE5D5),
            pageEdgeDark  = Color(0xFFC48472),
            seal          = Color(0xFFB8533F)
        ),
        typography = SkinTypography(
            titleFontFamily = FontFamily.Serif,
            bodyFontFamily  = FontFamily.Serif,
            titleLetterSpacingEm = 0.3f,
            bodyLetterSpacingEm  = 0.06f,
            titleEmboss = false
        ),
        materials = SkinMaterials(
            leatherNoiseCount = 1500,
            leatherNoiseAlpha = 0.025f,
            paperNoiseCount   = 800,
            paperNoiseAlpha   = 0.04f,
            paperFiberCount   = 60,
            paperFiberAlpha   = 0.06f,
            foilDoubleStroke  = false
        ),
        ornaments = SkinOrnaments(
            cornerOrnament = OrnamentType.Sakura,
            ribbonStyle    = RibbonStyle.Petal,
            claspStyle     = ClaspStyle.Pearl,
            showRings      = false,
            showSeal       = false
        ),
        effects = SkinEffects(
            particlesOnOpen = true,
            particleCount   = 12,
            breathing       = true,
            flipHapticEnabled = true
        )
    )

    /** 赤焰天书 —— 炽红 + 烫金 + 朱砂火印，焚天烈焰魔典。 */
    val ChiYan: BookSkin = BookSkin(
        id = SkinId.of("builtin", "chiyan"),
        meta = SkinMeta(
            displayNameRes = R.string.skin_chiyan_name,
            descriptionRes = R.string.skin_chiyan_desc,
            author = "FunLife",
            version = 1,
            unlock = Unlock.Free
        ),
        palette = SkinPalette(
            cover         = ColorPair(Color(0xFF6B0E0E), Color(0xFFB52424)),  // 炽红绒布
            coverShadow   = Color(0xFF3D0606),
            spine         = ColorPair(Color(0xFF4A0808), Color(0xFF6B0E0E)),
            paper         = Color(0xFFF7E5C8),
            paperFiber    = Color(0xFFC4793A),
            ink           = Color(0xFF3D0606),
            inkSoft       = Color(0xFF8B2828),
            foil          = ColorPair(Color(0xFFFFD75A), Color(0xFFC48A1A)),  // 烈金
            brass         = ColorPair(Color(0xFFFFB347), Color(0xFFFFE08A)),
            ribbon        = Color(0xFFFFD75A),
            ruling        = Color(0xFF8B2828),
            pageEdge      = Color(0xFFEDC07A),
            pageEdgeDark  = Color(0xFFA85B0F),
            seal          = Color(0xFFFFE08A)                                  // 烫金火印
        ),
        typography = SkinTypography(
            titleFontFamily = FontFamily.Serif,
            bodyFontFamily  = FontFamily.Serif,
            titleLetterSpacingEm = 0.45f,
            bodyLetterSpacingEm  = 0.10f,
            titleEmboss = true
        ),
        ornaments = SkinOrnaments(
            cornerOrnament = OrnamentType.Flame,
            ribbonStyle    = RibbonStyle.Jewel,
            claspStyle     = ClaspStyle.HexStar,
            showRings      = true,
            showSeal       = true
        ),
        effects = SkinEffects(
            particlesOnOpen = true,
            particleCount   = 32,
            breathing       = true,
            flipHapticEnabled = true
        )
    )

    /** 青鸾翠竹 —— 翡翠绿 + 烫银 + 竹纹，仙家清雅道经。 */
    val QingLuan: BookSkin = BookSkin(
        id = SkinId.of("builtin", "qingluan"),
        meta = SkinMeta(
            displayNameRes = R.string.skin_qingluan_name,
            descriptionRes = R.string.skin_qingluan_desc,
            author = "FunLife",
            version = 1,
            unlock = Unlock.Free
        ),
        palette = SkinPalette(
            cover         = ColorPair(Color(0xFF1F4D3A), Color(0xFF3A7A5C)),  // 翡翠绿
            coverShadow   = Color(0xFF143329),
            spine         = ColorPair(Color(0xFF143329), Color(0xFF1F4D3A)),
            paper         = Color(0xFFEAF1E5),                                 // 玉色纸
            paperFiber    = Color(0xFF7AAB8F),
            ink           = Color(0xFF143329),
            inkSoft       = Color(0xFF3A7A5C),
            foil          = ColorPair(Color(0xFFE5EAEC), Color(0xFFA8B4B0)),   // 烫银
            brass         = ColorPair(Color(0xFFB0BFB8), Color(0xFFFFFFFF)),
            ribbon        = Color(0xFFC9F0DA),
            ruling        = Color(0xFF3A7A5C),
            pageEdge      = Color(0xFFD8E5D2),
            pageEdgeDark  = Color(0xFF3A7A5C),
            seal          = Color(0xFFB23A48)
        ),
        typography = SkinTypography(
            titleFontFamily = FontFamily.Serif,
            bodyFontFamily  = FontFamily.Serif,
            titleLetterSpacingEm = 0.5f,
            bodyLetterSpacingEm  = 0.10f,
            titleEmboss = true
        ),
        ornaments = SkinOrnaments(
            cornerOrnament = OrnamentType.Bamboo,
            ribbonStyle    = RibbonStyle.Plain,
            claspStyle     = ClaspStyle.Pearl,
            showRings      = true,
            showSeal       = true
        ),
        effects = SkinEffects(
            particlesOnOpen = true,
            particleCount   = 18,
            breathing       = true,
            flipHapticEnabled = true
        )
    )

    /** 星河长卷 —— 深海宝蓝 + 星辰银箔，浩瀚星海航海志。 */
    val XingHe: BookSkin = BookSkin(
        id = SkinId.of("builtin", "xinghe"),
        meta = SkinMeta(
            displayNameRes = R.string.skin_xinghe_name,
            descriptionRes = R.string.skin_xinghe_desc,
            author = "FunLife",
            version = 1,
            unlock = Unlock.Free
        ),
        palette = SkinPalette(
            cover         = ColorPair(Color(0xFF0B1F4A), Color(0xFF1B3D7A)),  // 深海宝蓝
            coverShadow   = Color(0xFF050E2A),
            spine         = ColorPair(Color(0xFF050E2A), Color(0xFF0B1F4A)),
            paper         = Color(0xFFE6ECF5),                                 // 银河纸
            paperFiber    = Color(0xFF7AA0D8),
            ink           = Color(0xFF050E2A),
            inkSoft       = Color(0xFF1B3D7A),
            foil          = ColorPair(Color(0xFFE8ECF5), Color(0xFF8FA8C7)),   // 星辰银
            brass         = ColorPair(Color(0xFFB6C4DC), Color(0xFFFFFFFF)),
            ribbon        = Color(0xFF6FB3FF),
            ruling        = Color(0xFF1B3D7A),
            pageEdge      = Color(0xFFD6DEEC),
            pageEdgeDark  = Color(0xFF1B3D7A),
            seal          = Color(0xFF6FB3FF)
        ),
        typography = SkinTypography(
            titleFontFamily = FontFamily.Serif,
            bodyFontFamily  = FontFamily.Serif,
            titleLetterSpacingEm = 0.5f,
            bodyLetterSpacingEm  = 0.10f,
            titleEmboss = true
        ),
        ornaments = SkinOrnaments(
            cornerOrnament = OrnamentType.Stars,
            ribbonStyle    = RibbonStyle.Jewel,
            claspStyle     = ClaspStyle.HexStar,
            showRings      = true,
            showSeal       = true
        ),
        effects = SkinEffects(
            particlesOnOpen = true,
            particleCount   = 36,
            breathing       = true,
            flipHapticEnabled = true
        )
    )

    val all: List<BookSkin> = listOf(HengWu, JiYue, QingChuan, ChiYan, QingLuan, XingHe)
    val default: BookSkin = HengWu

    fun findById(id: SkinId): BookSkin? = all.firstOrNull { it.id == id }
}
