package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.robots.StringMapPointsDiff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object MapPoints {

    const val CUBE_SIZE: Double = 0.05

    fun CoroutineScope.launchSaveRoundedMapPointDiff(mapPointsDiff: StringMapPointsDiff) {
        launch {
            RedisWrapper.use {
                for (point in mapPointsDiff) {
                    val type = point[0]
                    val pointStr = point.substring(1)

                    hset("map", pointStr, type.toString())
                }
            }
        }
    }

    fun roundMapPointDiff(mapPointsDiff: StringMapPointsDiff): StringMapPointsDiff {
        return mapPointsDiff.mapTo(mutableSetOf()) { it[0] + roundCoords(it.substring(1)) }
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

    private fun roundCoord(x: Double): Double = Math.round(x / CUBE_SIZE) * CUBE_SIZE

    fun serializeMapPointsDiff(
        mapPointsDiff: StringMapPointsDiff,
    ): String {
        return mapPointsDiff.joinToString(" ")
    }

    fun deserializeMapPointsDiff(serialized: String): StringMapPointsDiff {
        val mapPointDiff = serialized.splitToSequence(' ').toSet()
        return mapPointDiff
    }

    fun deserializeAndRoundMapPointsDiff(serialized: String): StringMapPointsDiff {
        return roundMapPointDiff(deserializeMapPointsDiff(serialized))
    }
}