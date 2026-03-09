// DataExportImport.kt - 数据导入导出工具
package com.example.funlife.utils

import android.content.Context
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.Player
import com.example.funlife.data.model.SpinWheelTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DataExportImport(private val context: Context) {
    
    // 导出数据到JSON
    suspend fun exportData(
        anniversaries: List<Anniversary>,
        players: List<Player>,
        templates: List<SpinWheelTemplate>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject()
            
            // 导出纪念日
            val anniversariesArray = JSONArray()
            anniversaries.forEach { anniversary ->
                val item = JSONObject().apply {
                    put("id", anniversary.id)
                    put("name", anniversary.name)
                    put("date", anniversary.date)
                }
                anniversariesArray.put(item)
            }
            jsonObject.put("anniversaries", anniversariesArray)
            
            // 导出玩家
            val playersArray = JSONArray()
            players.forEach { player ->
                val item = JSONObject().apply {
                    put("id", player.id)
                    put("name", player.name)
                    put("score", player.score)
                }
                playersArray.put(item)
            }
            jsonObject.put("players", playersArray)
            
            // 导出转盘模板
            val templatesArray = JSONArray()
            templates.forEach { template ->
                val item = JSONObject().apply {
                    put("id", template.id)
                    put("name", template.name)
                    put("options", template.options)
                    put("category", template.category)
                    put("isDefault", template.isDefault)
                    put("usageCount", template.usageCount)
                    put("createdAt", template.createdAt)
                }
                templatesArray.put(item)
            }
            jsonObject.put("templates", templatesArray)
            
            // 添加元数据
            jsonObject.put("exportDate", LocalDateTime.now().toString())
            jsonObject.put("version", "1.0")
            
            // 保存到文件
            val fileName = "funlife_backup_${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            }.json"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(jsonObject.toString(2))
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 从JSON导入数据
    suspend fun importData(filePath: String): Result<ImportedData> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            val jsonString = file.readText()
            val jsonObject = JSONObject(jsonString)
            
            // 导入纪念日
            val anniversaries = mutableListOf<Anniversary>()
            val anniversariesArray = jsonObject.optJSONArray("anniversaries")
            anniversariesArray?.let {
                for (i in 0 until it.length()) {
                    val item = it.getJSONObject(i)
                    anniversaries.add(
                        Anniversary(
                            id = 0, // 让数据库自动生成新ID
                            name = item.getString("name"),
                            date = item.getString("date")
                        )
                    )
                }
            }
            
            // 导入玩家
            val players = mutableListOf<Player>()
            val playersArray = jsonObject.optJSONArray("players")
            playersArray?.let {
                for (i in 0 until it.length()) {
                    val item = it.getJSONObject(i)
                    players.add(
                        Player(
                            id = 0, // 让数据库自动生成新ID
                            name = item.getString("name"),
                            score = item.getInt("score")
                        )
                    )
                }
            }
            
            // 导入转盘模板
            val templates = mutableListOf<SpinWheelTemplate>()
            val templatesArray = jsonObject.optJSONArray("templates")
            templatesArray?.let {
                for (i in 0 until it.length()) {
                    val item = it.getJSONObject(i)
                    templates.add(
                        SpinWheelTemplate(
                            id = 0, // 让数据库自动生成新ID
                            name = item.getString("name"),
                            options = item.getString("options"),
                            category = item.optString("category", "custom"),
                            isDefault = item.optBoolean("isDefault", false),
                            usageCount = item.optInt("usageCount", 0),
                            createdAt = item.optString("createdAt", "")
                        )
                    )
                }
            }
            
            Result.success(ImportedData(anniversaries, players, templates))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class ImportedData(
        val anniversaries: List<Anniversary>,
        val players: List<Player>,
        val templates: List<SpinWheelTemplate>
    )
}
