package com.example.funlife.game.platformer.catalog

import com.example.funlife.game.platformer.PlatformerCharacterId
import java.util.LinkedHashSet

/**
 * 横版角色动画内存池：约 15% 堆预算，保留当前 + 最近 2 个热角色，其余仅磁盘。
 */
object PlatformerAnimMemoryPool {

    private const val HOT_SLOT_COUNT = 3
    private const val HEAP_BUDGET_FRACTION = 0.15

    private val hotCharacters = LinkedHashSet<PlatformerCharacterId>(HOT_SLOT_COUNT + 1)

    fun budgetBytes(): Long =
        (Runtime.getRuntime().maxMemory().toDouble() * HEAP_BUDGET_FRACTION).toLong()

    fun budgetMb(): Int = (budgetBytes() / (1024 * 1024)).toInt().coerceAtLeast(32)

    fun onCharacterFocused(characterId: PlatformerCharacterId) {
        synchronized(hotCharacters) {
            hotCharacters.remove(characterId)
            hotCharacters.add(characterId)
            while (hotCharacters.size > HOT_SLOT_COUNT) {
                val evicted = hotCharacters.first()
                hotCharacters.remove(evicted)
                if (evicted != characterId) {
                    PlatformerRemoteAnimCache.releaseCharacterMemory(evicted)
                }
            }
        }
    }

    fun shouldRetainInMemory(characterId: PlatformerCharacterId): Boolean =
        synchronized(hotCharacters) { characterId in hotCharacters }

    fun hotCharacters(): Set<PlatformerCharacterId> =
        synchronized(hotCharacters) { hotCharacters.toSet() }

    fun evictColdCharacters(keep: Set<PlatformerCharacterId>) {
        synchronized(hotCharacters) {
            val toEvict = hotCharacters.filter { it !in keep }
            hotCharacters.retainAll(keep)
            toEvict.forEach { PlatformerRemoteAnimCache.releaseCharacterMemory(it) }
        }
    }

    fun reset() {
        synchronized(hotCharacters) {
            hotCharacters.toList().forEach { PlatformerRemoteAnimCache.releaseCharacterMemory(it) }
            hotCharacters.clear()
        }
    }
}
