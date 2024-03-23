package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.redis.MapPoints.launchSaveRoundedMapPointDiff
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class Robot(val id: String) {
    // we add SupervisorJob to the scope to avoid cancelling the whole scope when one of the jobs fails (the class
    // instance does not become invalid if one of the jobs fails)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var lastReceivedKeepAlive: Long = 0

    private val updateMapSolidSubscribers = ConcurrentHashMap<Any, suspend (StringMapPointsDiff) -> Unit>()

    private val updatePosSubscribers = ConcurrentHashMap<Any, suspend (String) -> Unit>()

    var lastPosString: String = "0,0,0"
        private set

    fun updateMap(message: String) {
        try {
            val roundedMapPointDiff = MapPoints.roundMapPointDiff(DataSubscribers.deserializeMapPointsDiff(message))
            notifyAllMapSolidSubscribers(roundedMapPointDiff)

            scope.launchSaveRoundedMapPointDiff(roundedMapPointDiff)

        } catch (e: IllegalArgumentException) {
            logger.warn("Received invalid message on channel $id update_map: $e\nmessage: $message")
        }
    }

    fun updatePos(message: String) {
        lastPosString = message
        notifyAllPosSubscribers(message)
    }

    fun subscribeToUpdateMapSolid(subscriber: Any, callback: suspend (StringMapPointsDiff) -> Unit) {
        updateMapSolidSubscribers[subscriber] = callback
    }

    fun subscribeToUpdatePos(subscriber: Any, callback: suspend (String) -> Unit) {
        updatePosSubscribers[subscriber] = callback
    }

    /**
     * Notify all subscribers with the given value. Blocking.
     */
    private fun notifyAllMapSolidSubscribers(mapPointsDiff: StringMapPointsDiff) =
        runBlocking {
            for (subscriber in updateMapSolidSubscribers.values) {
                launch { subscriber(mapPointsDiff) }
            }
        }

    private fun notifyAllPosSubscribers(pos: String) =
        runBlocking {
            for (subscriber in updatePosSubscribers.values) {
                launch { subscriber(pos) }
            }
        }


    suspend fun suspendRemove() {
        notifyAllPosSubscribers("remove")

        logger.debug("Unsubscribing from Redis channels for robot $id")
        RedisWrapper.use {
            srem("robots", id)
        }

        scope.cancel()
    }

    companion object {
        const val KEEP_ALIVE_TIMEOUT: Long = 1 * 1000L

        /**
         * Remove robots that didn't send a keep-alive in the last [KEEP_ALIVE_TIMEOUT] milliseconds.
         */
        suspend fun MutableMap<String, Robot>.filterRobots() {
            val now = System.currentTimeMillis()
            val iterator = this.iterator()
            while (iterator.hasNext()) {
                val robot = iterator.next().value
                if (now - robot.lastReceivedKeepAlive > KEEP_ALIVE_TIMEOUT) {
                    logger.info("Robot ${robot.id} disconnected (no keep-alive received for 1 second)")

                    robot.suspendRemove()

                    iterator.remove()
                }
            }
        }
    }
}   