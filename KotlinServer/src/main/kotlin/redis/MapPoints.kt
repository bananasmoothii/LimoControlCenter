package fr.bananasmoothii.limocontrolcenter.redis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object MapPoints {

    fun CoroutineScope.launchSaveMapPointDiff(mapPointsDiff: StringMapPointsDiff) {
        launch {
            RedisWrapper.use {
                for (point in mapPointsDiff) {
                    val type = point[0]
                    val pointStr = point.substring(1)

                    hset("map:solid", pointStr, type.toString())
                }
            }
        }
    }
}

enum class MapPointType(val letter: Char) {
    WALL('W'),
    UNKNOWN('U'),
    PASSABLE('P');

    companion object {
        fun fromLetter(letter: Char): MapPointType {
            return entries.find { it.letter == letter } ?: error("Unknown MapPointType letter: $letter")
        }
    }
}