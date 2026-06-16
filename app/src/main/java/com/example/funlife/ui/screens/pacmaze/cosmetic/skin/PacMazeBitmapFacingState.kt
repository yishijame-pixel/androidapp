package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

/**
 * 位图皮肤绘制朝向记忆（仅渲染，不改逻辑移动）。
 * - 移动中：跟速度 / 输入方向
 * - 停止后：保留最后一次有效朝向
 * - 关卡开局：展示原图自然朝向（朝左 PNG 不镜像）
 */
internal object PacMazeBitmapFacingState {

    private val lastFacingByEntity = mutableMapOf<String, Direction>()

    fun clear(entityId: String) {
        lastFacingByEntity.remove(entityId)
    }

    fun clearAll() {
        lastFacingByEntity.clear()
    }

    fun resolve(skinId: PacMazeSkinId, entity: PacMazeEntity): Direction {
        val fromVelocity = PacMazeEntityVisuals.facingFromVelocity(entity.velX, entity.velY)
        val activeTravel = fromVelocity ?: entity.direction

        if (PacMazeEntityVisuals.isLocomoting(entity) && activeTravel != null) {
            lastFacingByEntity[entity.id] = activeTravel
            return activeTravel
        }

        if (fromVelocity != null) {
            lastFacingByEntity[entity.id] = fromVelocity
            return fromVelocity
        }

        lastFacingByEntity[entity.id]?.let { return it }

        val natural = PacMazeSkinRenderProfileCatalog.naturalIdleFacing(skinId)
        lastFacingByEntity[entity.id] = natural
        return natural
    }
}
