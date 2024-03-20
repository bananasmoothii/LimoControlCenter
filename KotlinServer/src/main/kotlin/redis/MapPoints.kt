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

                    hset("map:solid", roundCoords(pointStr), type.toString())
                }
            }
        }
    }

    fun roundCoords(coordsStr: String): String {
        val coords = coordsStr.split(",")
        val x = coords[0].toDouble()
        val y = coords[1].toDouble()
        if (coords.size == 2) {
            return "${roundCoord(x)},${roundCoord(y)}"
        } else {
            val z = coords[2].toDouble()
            return "${roundCoord(x)},${roundCoord(y)},${roundCoord(z)}"
        }
    }

    private fun roundCoord(x: Double): Double = Math.round(x * 10.0) / 10.0
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