package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import kotlinx.coroutines.*

typealias MapPointsDiff = Pair<Set<String>?, Set<String>?>

object DataSubscribers {

    val updateMapSolidSubscribers = mutableMapOf<Any, suspend (mapPointsDiff: MapPointsDiff) -> Unit>()

    private val addPointsFromSubscribeToSetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun subscribeToUpdateChannels() {
        RedisWrapper.subscribe("update:map:solid") { channel, message ->
            try {
                val mapPointsDiff = deserializeMapPointsDiff(message)
                updateMapSolidSubscribers.notifyAllSubscribers(mapPointsDiff)

                addPointsFromSubscribeToSetScope.launch {
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
            } catch (e: IllegalArgumentException) {
                logger.warn("Received invalid message on channel $channel: $e\nmessage: $message")
            }
        }
    }

    /**
     * Notify all subscribers with the given value. Blocking.
     */
    private fun Map<Any, suspend (MapPointsDiff) -> Unit>.notifyAllSubscribers(mapPointsDiff: MapPointsDiff) =
        runBlocking {
            for (subscriber in values) {
                launch { subscriber(mapPointsDiff) }
            }
        }

    fun serializeMapPointsDiff(
        pointsToAdd: Collection<String>?,
        pointsToRemove: Collection<String>?,
        addZCoord: Boolean = false
    ): String {
        val builder = StringBuilder(5 * ((pointsToAdd?.size ?: 0) + (pointsToRemove?.size ?: 0)) + 1)
        if (pointsToAdd != null) {
            for (point in pointsToAdd) {
                builder.append(point)
                if (point.count { it == ',' } <= 2 && addZCoord) builder.append(",0.1")
                builder.append(' ')
            }
        }
        builder.append('/')
        if (pointsToRemove != null) {
            for (point in pointsToRemove) {
                builder.append(point)
                if (point.count { it == ',' } <= 2 && addZCoord) builder.append(",0.1")
                builder.append(' ')
            }
        }
        return builder.toString()
    }

    fun deserializeMapPointsDiff(serialized: String): MapPointsDiff {
        val (addStr, removeStr) = serialized.split('/', limit = 2).also {
            require(it.size == 2) { "Serialized map points diff should contain exactly one slash separator" }
        }
        val add = if (addStr.isEmpty()) null else addStr.splitToSequence(' ').toSet()
        val remove = if (removeStr.isEmpty()) null else removeStr.splitToSequence(' ').toSet()
        return add to remove
    }
}
