package com.example.funlife.ui.screens.platformer

/**
 * 面向玩家的加载文案：屏蔽「精灵图 / 序列帧 / 解码 / 帧」等开发术语，
 * 统一为「游戏资源 / 角色资源」等产品化表述。
 */
object GameResourceLoadCopy {

    private val clipLabel = mapOf(
        "walk" to "动作",
        "jump" to "跳跃",
        "idle" to "形象",
        "die" to "特效",
        "run" to "奔跑",
        "attack" to "攻击",
    )

    fun forDisplay(raw: String): String {
        if (raw.isBlank()) return raw
        var s = raw.trim()

        clipLabel.forEach { (key, label) ->
            s = s.replace(Regex("${key}\\s*至磁盘", RegexOption.IGNORE_CASE), "整理${label}资源")
            s = s.replace(Regex("编码\\s*${key}", RegexOption.IGNORE_CASE), "整理${label}资源")
            s = s.replace(Regex("整理\\s*${key}", RegexOption.IGNORE_CASE), "整理${label}资源")
            s = s.replace(Regex("解析\\s*${key}", RegexOption.IGNORE_CASE), "解析${label}资源")
        }

        val replacements = listOf(
            "云端多段动作" to "多段动作",
            "云端行走角色" to "梗图行走角色",
            "一十云端角色" to "一十品牌角色",
            "云端动画" to "角色资源",
            "云端动画包" to "角色资源包",
            "横版冒险" to "坤坤大冒险",
            "横版资源" to "坤坤大冒险资源",
            "横版角色" to "坤坤大冒险角色",
            "同步横版资源" to "同步坤坤大冒险资源",
            "横版资源待同步" to "坤坤大冒险资源待同步",
            "同步云端清单" to "同步资源清单",
            "云端行走" to "行走角色",
            "加载行走序列帧" to "加载角色动作资源",
            "加载行走精灵图" to "加载角色动作资源",
            "加载跳跃精灵图" to "加载角色跳跃资源",
            "加载待机精灵图" to "加载角色形象资源",
            "加载行走小鸡精灵图" to "加载行走小鸡资源",
            "编码行走小鸡" to "整理行走小鸡资源",
            "精灵图就绪" to "角色资源已就绪",
            "精灵图加载中" to "角色资源加载中",
            "读取动画缓存" to "读取本地资源",
            "读取行走缓存" to "读取动作资源",
            "读取跳跃缓存" to "读取跳跃资源",
            "解码可玩帧" to "加载角色资源",
            "可玩帧不足" to "资源加载中",
            "可玩就绪" to "资源已就绪",
            "解码行走动画" to "加载角色动作资源",
            "解码跳跃动画" to "加载角色跳跃资源",
            "动画解码未完成" to "角色资源未完成",
            "动画缓存命中" to "本地资源命中",
            "行走动画就绪" to "角色动作资源已就绪",
            "封面解码失败" to "封面资源加载失败",
            "检查动画缓存" to "检查游戏资源",
            "动画资源不完整" to "角色资源不完整",
            "动画加载未完成" to "资源加载未完成",
            "角色动画资源" to "角色游戏资源",
            "角色动画" to "角色资源",
            "解析全动画" to "解析角色资源",
            "后台编码其余角色" to "后台整理其余角色资源",
            "后台编码" to "后台整理资源",
            "编码 " to "整理 ",
            "精灵图" to "角色资源",
            "序列帧" to "资源",
            "动画图" to "资源",
            "动画缓存" to "资源缓存",
            "可玩帧" to "资源",
            "角色动画" to "角色资源",
            "动画后台加载" to "资源后台加载",
            "加载动画" to "加载资源",
            "读取动画" to "加载资源",
            "解码" to "加载",
        )
        for ((from, to) in replacements) {
            s = s.replace(from, to)
        }

        s = s.replace(Regex("""\(\s*\d+\s*帧\s*\)"""), "")
        s = s.replace(Regex("""\(\s*\d+\s*/\s*\d+\s*\)"""), "")
        s = s.replace(Regex("""\s+至磁盘…?"""), "…")
        s = s.replace(Regex("""整理\s+\w+\s+\d+/\d+…?"""), "整理角色资源…")
        s = s.replace(Regex("""解析\s+\w+…"""), "解析角色资源…")
        s = s.replace(Regex("""\s{2,}"""), " ")
        s = s.trim(' ', '·', '…', '.')
        if (s.isEmpty()) return "加载游戏资源…"
        if (!s.endsWith("…") && !s.endsWith("。") && !s.endsWith("！") && !s.endsWith("%")) {
            if (s.contains("加载") || s.contains("整理") || s.contains("读取") || s.contains("准备")) {
                s = "$s…"
            }
        }
        return s
    }

    fun phaseLabel(raw: String): String = forDisplay(raw)

    fun progress(label: String, loaded: Int, total: Int): String =
        forDisplay("$label $loaded/$total")
}
