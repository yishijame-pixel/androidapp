// PlayerRepository.kt - 玩家仓库
package com.example.funlife.repository

import android.util.Log
import com.example.funlife.data.dao.PlayerDao
import com.example.funlife.data.model.Player
import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {
    
    val allPlayers: Flow<List<Player>> = playerDao.getAllPlayers()
    
    suspend fun insert(player: Player) {
        Log.d("PlayerRepository", "Inserting player: $player")
        playerDao.insertPlayer(player)
    }
    
    suspend fun update(player: Player) {
        Log.d("PlayerRepository", "Updating player in DB: ID=${player.id}, Name=${player.name}, Score=${player.score}")
        playerDao.updatePlayer(player)
        Log.d("PlayerRepository", "Player updated successfully")
    }
    
    suspend fun delete(player: Player) {
        Log.d("PlayerRepository", "Deleting player: $player")
        playerDao.deletePlayer(player)
    }
    
    suspend fun resetAllScores() {
        Log.d("PlayerRepository", "Resetting all scores")
        playerDao.resetAllScores()
    }
}
