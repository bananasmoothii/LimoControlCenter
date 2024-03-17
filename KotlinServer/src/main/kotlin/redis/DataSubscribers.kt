package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

typealias MapPointsDiff = Pair<Set<String>?, Set<String>?>

object DataSubscribers {

    val updateMapSolidSubscribers = mutableMapOf<Any, suspend (mapPointsDiff: MapPointsDiff) -> Unit>()

    fun subscribeToUpdateChannels() {
        RedisWrapper.subscribe("update:map:solid") { channel, message ->
            try {
                val mapPointsDiff = deserializeMapPointsDiff(message)
                updateMapSolidSubscribers.notifyAllSubscribers(mapPointsDiff)
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

    fun serializeMapPointsDiff(pointsToAdd: Collection<String>?, pointsToRemove: Collection<String>?): String {
        val builder = StringBuilder(5 * ((pointsToAdd?.size ?: 0) + (pointsToRemove?.size ?: 0)) + 1)
        if (pointsToAdd != null) {
            for (point in pointsToAdd) {
                builder.append(point).append(' ')
            }
        }
        builder.append('\n')
        if (pointsToRemove != null) {
            for (point in pointsToRemove) {
                builder.append(point).append(' ')
            }
        }
        return builder.toString()
    }

    fun deserializeMapPointsDiff(serialized: String): MapPointsDiff {
        val (addStr, removeStr) = serialized.split('\n', limit = 2).also {
            require(it.size == 2) { "Serialized map points diff should contain exactly one newline" }
        }
        val add = if (addStr.isEmpty()) null else addStr.splitToSequence(' ').toSet()
        val remove = if (removeStr.isEmpty()) null else removeStr.splitToSequence(' ').toSet()
        return add to remove
    }
}
