package com.example.funlife.game.platformer



/**

 * CI 关卡校验：长度预算、脚本完整性、连通性（出生点 + 终点）。

 */

object PlatformerLevelValidator {



    data class ValidationIssue(

        val levelId: Int,

        val code: String,

        val message: String,

    )



    data class Report(

        val issues: List<ValidationIssue>,

        val totalTargetTiles: Int,

        val levelCount: Int,

    ) {

        val ok: Boolean get() = issues.isEmpty()

    }



    fun validateAll(levels: List<PlatformerLevelDef> = PlatformerLevels.all): Report {

        val issues = mutableListOf<ValidationIssue>()

        for (level in levels) {

            issues += validateManifest(level)

        }

        return Report(

            issues = issues,

            totalTargetTiles = PlatformerCampaignLengthSpec.totalCampaignTiles() +

                PlatformerSkyLengthSpec.totalSkyTiles(),

            levelCount = levels.size,

        )

    }



    fun validateManifest(level: PlatformerLevelDef): List<ValidationIssue> {

        if (PlatformerSkyLevelCatalog.isSkyLevel(level.id)) {

            return validateSkyManifest(level)

        }

        val issues = mutableListOf<ValidationIssue>()

        val budget = PlatformerCampaignLengthSpec.budget(level.id)

        val script = level.campaignSegmentScript



        if (script.isNullOrEmpty()) {

            issues += issue(level.id, "NO_SCRIPT", "缺少 campaignSegmentScript")

            return issues

        }

        if (script.first().kind != PlatformerSegmentLibrary.SegmentKind.ENTRY) {

            issues += issue(level.id, "BAD_ENTRY", "首段必须为 ENTRY")

        }

        if (script.last().kind != PlatformerSegmentLibrary.SegmentKind.FINALE) {

            issues += issue(level.id, "BAD_FINALE", "末段必须为 FINALE")

        }

        if (script.size != budget.segmentCount) {

            issues += issue(

                level.id, "SEGMENT_COUNT",

                "脚本段数 ${script.size} != 预算 ${budget.segmentCount}",

            )

        }

        if (level.targetTiles != budget.targetTiles) {

            issues += issue(

                level.id, "TARGET_TILES",

                "targetTiles ${level.targetTiles} != ${budget.targetTiles}",

            )

        }

        if (level.tmxAsset != null) {

            issues += issue(level.id, "LEGACY_TMX", "禁止独立 tmxAsset 短关；请用 STORY_ROOM 嵌入")

        }

        if (level.useCampaignScroll != budget.useScrollBuffer) {

            issues += issue(level.id, "SCROLL_FLAG", "useCampaignScroll 与预算不一致")

        }

        val estMinutes = budget.estimatedMinutesAtPace

        if (estMinutes < budget.targetMinutesMin * 0.85f) {

            issues += issue(

                level.id, "TOO_SHORT",

                "预估 ${"%.1f".format(estMinutes)}min < KPI ${budget.targetMinutesMin}min",

            )

        }

        return issues

    }



    private fun validateSkyManifest(level: PlatformerLevelDef): List<ValidationIssue> {

        val issues = mutableListOf<ValidationIssue>()

        val budget = PlatformerSkyLengthSpec.budget(level.id)

        val script = level.campaignSegmentScript



        if (script.isNullOrEmpty()) {

            issues += issue(level.id, "NO_SCRIPT", "缺少 campaignSegmentScript")

            return issues

        }

        if (script.first().kind != PlatformerSegmentLibrary.SegmentKind.SKY_ENTRY) {

            issues += issue(level.id, "BAD_SKY_ENTRY", "高空关首段必须为 SKY_ENTRY")

        }

        if (script.last().kind != PlatformerSegmentLibrary.SegmentKind.SKY_FINALE) {

            issues += issue(level.id, "BAD_SKY_FINALE", "高空关末段必须为 SKY_FINALE")

        }

        if (script.size != budget.segmentCount) {

            issues += issue(

                level.id, "SEGMENT_COUNT",

                "脚本段数 ${script.size} != 预算 ${budget.segmentCount}",

            )

        }

        if (level.targetTiles != budget.targetTiles) {

            issues += issue(

                level.id, "TARGET_TILES",

                "targetTiles ${level.targetTiles} != ${budget.targetTiles}",

            )

        }

        if (level.tmxAsset != null) {

            issues += issue(level.id, "LEGACY_TMX", "高空关禁止独立 tmxAsset")

        }

        if (level.useCampaignScroll != budget.useScrollBuffer) {

            issues += issue(level.id, "SCROLL_FLAG", "useCampaignScroll 与预算不一致")

        }

        val estMinutes = budget.estimatedMinutesAtPace

        if (estMinutes < budget.targetMinutesMin * 0.85f) {

            issues += issue(

                level.id, "TOO_SHORT",

                "预估 ${"%.1f".format(estMinutes)}min < KPI ${budget.targetMinutesMin}min",

            )

        }

        return issues

    }



    /** 烘焙后校验：出生点、终点格存在。 */

    fun validateWorld(world: PlatformerWorld): List<ValidationIssue> {

        val issues = mutableListOf<ValidationIssue>()

        var hasSpawn = false

        var hasGoal = false

        for (y in 0 until world.height) {

            for (x in 0 until world.width) {

                when (world.cellAt(x, y)) {

                    PlatformerCell.SPAWN -> hasSpawn = true

                    PlatformerCell.GOAL -> hasGoal = true

                    else -> Unit

                }

            }

        }

        if (!hasSpawn && world.player.x <= 0f) {

            issues += issue(world.level.id, "NO_SPAWN", "未找到出生点")

        }

        if (!hasGoal && world.goalX == null) {

            issues += issue(world.level.id, "NO_GOAL", "未找到终点")

        }

        return issues

    }



    private fun issue(levelId: Int, code: String, message: String) =

        ValidationIssue(levelId, code, message)

}


