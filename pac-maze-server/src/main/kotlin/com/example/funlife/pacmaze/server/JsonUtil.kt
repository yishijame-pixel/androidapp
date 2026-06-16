package com.example.funlife.pacmaze.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtil {
    private val gson = Gson()

    fun toJson(obj: Any): String = gson.toJson(obj)

    fun toMap(obj: Any): Map<String, Any?> {
        val json = gson.toJson(obj)
        return parseMap(json)
    }

    fun parseMap(json: String): Map<String, Any?> {
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(json, type)
    }

    fun parseMessage(json: String): Map<String, Any?> = parseMap(json)
}
