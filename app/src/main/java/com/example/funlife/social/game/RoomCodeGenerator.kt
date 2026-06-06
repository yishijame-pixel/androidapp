package com.example.funlife.social.game

import kotlin.random.Random

object RoomCodeGenerator {

    private const val CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LENGTH = 6

    fun generate(): String = buildString(LENGTH) {
        repeat(LENGTH) {
            append(CHARSET[Random.nextInt(CHARSET.length)])
        }
    }
}
