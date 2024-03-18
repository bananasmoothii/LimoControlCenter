package fr.bananasmoothii.limocontrolcenter.redis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object MapPoints {
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun saveMapPointDiffNonBlock(mapPointsDiff: MapPointsDiff) {
        ioScope.launch {
            RedisWrapper.use {
                val (pointsToAdd, pointsToRemove) = mapPointsDiff
                if (pointsToAdd != null) {
                    for (point in pointsToAdd) {
                        sadd("map:solid", point)
                    }
                }
                if (pointsToRemove != null) {
                    for (point in pointsToRemove) {
                        srem("map:solid", point)
                    }
                }
            }
        }
    }
}