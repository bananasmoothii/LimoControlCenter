package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.redis.MapPoints.launchSaveMapPointDiff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import redis.clients.jedis.JedisPubSub

class Robot(val id: String) : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    var lastReceivedKeepAlive: Long = 0

    private val jedisSubscribers = mutableListOf<JedisPubSub>()

    private val updateMapSolidSubscribers = mutableMapOf<Any, suspend (MapPointsDiff) -> Unit>()

    init {
        val solidMapPointsSubscriber = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                logger.debug("Received message on channel $channel: $message")
                try {
                    val mapPointsDiff = DataSubscribers.deserializeMapPointsDiff(message)
                    notifyAllMapSolidSubscribers(mapPointsDiff)

                    launchSaveMapPointDiff(mapPointsDiff)

                } catch (e: IllegalArgumentException) {
                    logger.warn("Received invalid message on channel $channel: $e\nmessage: $message")
                }
            }
        }

        logger.debug("Subscribing to channel: $id update:map:solid")
        RedisWrapper.subscribe(solidMapPointsSubscriber, "$id update:map:solid")
    }

    fun subscribeToUpdateMapSolid(subscriber: Any, callback: suspend (MapPointsDiff) -> Unit) {
        updateMapSolidSubscribers[subscriber] = callback
    }

    /**
     * Notify all subscribers with the given value. Blocking.
     */
    private fun notifyAllMapSolidSubscribers(mapPointsDiff: MapPointsDiff) =
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