package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.ChatPersona
import com.example.funlife.data.model.ChatPersonaState
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatPersonaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: ChatPersona)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonaState(state: ChatPersonaState)

    @Query("SELECT * FROM chat_personas ORDER BY sortOrder")
    fun getAllPersonas(): Flow<List<ChatPersona>>

    @Query("SELECT * FROM chat_personas ORDER BY sortOrder")
    suspend fun getAllPersonasList(): List<ChatPersona>

    @Query("SELECT * FROM chat_personas WHERE id = :id")
    suspend fun getPersonaById(id: String): ChatPersona?

    @Query("SELECT * FROM chat_persona_state WHERE personaId = :personaId AND userId = :userId")
    suspend fun getPersonaState(personaId: String, userId: Long): ChatPersonaState?

    @Update
    suspend fun updatePersonaState(state: ChatPersonaState)

    @Query("UPDATE chat_persona_state SET interactionCount = interactionCount + 1 WHERE personaId = :personaId AND userId = :userId")
    suspend fun incrementInteraction(personaId: String, userId: Long)

    @Query("UPDATE chat_persona_state SET affection = :affection WHERE personaId = :personaId AND userId = :userId")
    suspend fun updateAffection(personaId: String, userId: Long, affection: Int)

    @Delete
    suspend fun deletePersona(persona: ChatPersona)

    @Query("SELECT COUNT(*) FROM chat_personas")
    suspend fun getPersonaCount(): Int

    @Query("UPDATE chat_personas SET customAvatarUri = :uri WHERE id = :personaId")
    suspend fun updateCustomAvatar(personaId: String, uri: String?)
}
