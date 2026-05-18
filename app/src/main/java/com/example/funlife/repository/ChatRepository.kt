package com.example.funlife.repository

import com.example.funlife.data.dao.BillDao
import com.example.funlife.data.dao.ChatMessageDao
import com.example.funlife.data.dao.ChatPersonaDao
import com.example.funlife.data.model.Bill
import com.example.funlife.data.model.ChatMessage
import com.example.funlife.data.model.ChatPersona
import com.example.funlife.data.model.ChatPersonaState
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val billDao: BillDao,
    private val messageDao: ChatMessageDao,
    private val personaDao: ChatPersonaDao
) {
    // ===== 账单 =====
    suspend fun insertBill(bill: Bill): Long = billDao.insert(bill)
    fun getAllBills(userId: Long): Flow<List<Bill>> = billDao.getAllBills(userId)
    suspend fun getRecentBills(userId: Long, limit: Int = 30) = billDao.getRecentBills(userId, limit)
    suspend fun getBillsByDateRange(userId: Long, start: Long, end: Long) = billDao.getBillsByDateRange(userId, start, end)
    suspend fun getTotalAmount(userId: Long, start: Long, end: Long) = billDao.getTotalAmount(userId, start, end) ?: 0.0
    suspend fun getCategoryCount(userId: Long, category: String, since: Long) = billDao.getCategoryCount(userId, category, since)
    suspend fun getBillById(id: Long) = billDao.getBillById(id)

    // ===== 消息 =====
    suspend fun insertMessage(message: ChatMessage): Long = messageDao.insert(message)
    fun getAllMessages(userId: Long): Flow<List<ChatMessage>> = messageDao.getAllMessages(userId)
    suspend fun getRecentMessages(userId: Long, limit: Int = 50) = messageDao.getRecentMessages(userId, limit)
    suspend fun clearMessages(userId: Long) = messageDao.clearAll(userId)
    suspend fun deleteMessage(id: Long) = messageDao.deleteById(id)
    fun searchMessages(userId: Long, query: String) = messageDao.searchMessages(userId, query)

    // ===== 账单操作 =====
    suspend fun deleteBill(bill: Bill) = billDao.delete(bill)
    suspend fun updateBill(bill: Bill) = billDao.update(bill)

    // ===== 人格 =====
    fun getAllPersonas(): Flow<List<ChatPersona>> = personaDao.getAllPersonas()
    suspend fun getAllPersonasList() = personaDao.getAllPersonasList()
    suspend fun getPersonaById(id: String) = personaDao.getPersonaById(id)
    suspend fun insertPersona(persona: ChatPersona) = personaDao.insertPersona(persona)
    suspend fun deletePersona(persona: ChatPersona) = personaDao.deletePersona(persona)
    suspend fun getPersonaCount() = personaDao.getPersonaCount()
    suspend fun updateCustomAvatar(personaId: String, uri: String?) = personaDao.updateCustomAvatar(personaId, uri)

    // ===== 人格状态 =====
    suspend fun getPersonaState(personaId: String, userId: Long) = personaDao.getPersonaState(personaId, userId)
    suspend fun insertPersonaState(state: ChatPersonaState) = personaDao.insertPersonaState(state)
    suspend fun incrementInteraction(personaId: String, userId: Long) = personaDao.incrementInteraction(personaId, userId)
    suspend fun updateAffection(personaId: String, userId: Long, affection: Int) = personaDao.updateAffection(personaId, userId, affection)
}
