package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.redis.MapPoints.launchSaveRoundedMapPointDiff
import kotlinx.coroutines.*
import redis.clients.jedis.JedisPubSub
import java.util.concurrent.ConcurrentHashMap

class Robot(val id: String) {
    // we add SupervisorJob to the scope to avoid cancelling the whole scope when one of the jobs fails (the class
    // instance does not become invalid if one of the jobs fails)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var lastReceivedKeepAlive: Long = 0

    private val jedisSubscribers = mutableListOf<JedisPubSub>()

    private val updateMapSolidSubscribers = ConcurrentHashMap<Any, suspend (StringMapPointsDiff) -> Unit>()

    init {
        val solidMapPointsSubscriber = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                logger.debug("Received message on channel $channel: $message")
                try {
                    val roundedMapPointDiff =
                        MapPoints.roundMapPointDiff(DataSubscribers.deserializeMapPointsDiff(message))
                    notifyAllMapSolidSubscribers(roundedMapPointDiff)

                    scope.launchSaveRoundedMapPointDiff(roundedMapPointDiff)

                } catch (e: IllegalArgumentException) {
                    logger.warn("Received invalid message on channel $channel: $e\nmessage: $message")
                }
            }
        }

        logger.debug("Subscribing to channel: $id update_map")
        RedisWrapper.subscribe(solidMapPointsSubscriber, "$id update_map")
    }

    fun subscribeToUpdateMapSolid(subscriber: Any, callback: suspend (StringMapPointsDiff) -> Unit) {
        updateMapSolidSubscribers[subscriber] = callback
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


    suspend fun suspendRemove() {
        RedisWrapper.use {
            srem("robots", id)
        }

        for (subscriber in jedisSubscribers) {
            subscriber.unsubscribe()
        }

        scope.cancel()
    }

    companion object {
        const val KEEP_ALIVE_TIMEOUT: Long = 10 * 1000L

        /**
         * Remove robots that didn't send a keep-alive in the last [KEEP_ALIVE_TIMEOUT] milliseconds.
         */
        suspend fun MutableMap<String, Robot>.filterRobots() {
            val now = System.currentTimeMillis()
            val iterator = this.iterator()
            while (iterator.hasNext()) {
                val robot = iterator.next().value
                if (now - robot.lastReceivedKeepAlive > KEEP_ALIVE_TIMEOUT) {
                    logger.info("Robot ${robot.id} disconnected (no keep-alive received for 10 seconds)")

                    robot.suspendRemove()

                    iterator.remove()
                }
            }
        }
    }
}