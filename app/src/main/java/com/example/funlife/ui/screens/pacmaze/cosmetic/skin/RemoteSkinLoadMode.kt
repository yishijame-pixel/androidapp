package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

/** 云端皮肤加载策略：网格封面 / 选中后全动画 */
enum class RemoteSkinLoadMode {
    /** 仅加载 preview.png，供皮肤网格快速展示 */
    CoverOnly,
    /** 加载完整序列帧（原图），供选中预览与局内 */
    FullAnimation,
}
