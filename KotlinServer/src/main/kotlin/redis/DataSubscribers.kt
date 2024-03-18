package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import kotlinx.coroutines.*
import redis.clients.jedis.JedisPubSub

typealias MapPointsDiff = Pair<Set<String>?, Set<String>?>

object DataSubscribers {

    /**
     * map of robotId to a pair of last keep-alive timestamp and JedisPubSubs
     */
    val robots = mutableMapOf<String, Pair<Long, MutableList<JedisPubSub>>>()

    val updateMapSolidSubscribers = mutableMapOf<Any, suspend (mapPointsDiff: MapPointsDiff) -> Unit>()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        ioScope.launch {
            while (true) {
                filterRobots()
                delay(1000)
            }
        }
    }

    fun subscribeToUpdateChannels() {
        RedisWrapper.subscribe("general") { _, message ->
            logger.debug("Received message on channel general: $message")
            val (command, param) = message.split(' ', limit = 2)

            when (command) {
                "keep_alive" -> {
                    val robotId = param

                    val previous = robots.put(robotId, System.currentTimeMillis() to mutableListOf())
                    if (previous == null) {
                        logger.info("New robot connected: $robotId")

                        ioScope.launch {
                            RedisWrapper.use {
                                this.sadd("robots", robotId)
                            }
                        }

                        val solidMapPointsSubscriber = object : JedisPubSub() {
                            override fun onMessage(channel: String, message: String) {
                                try {
                                    val mapPointsDiff = deserializeMapPointsDiff(message)
                                    updateMapSolidSubscribers.notifyAllSubscribers(mapPointsDiff)

                                    MapPoints.saveMapPointDiffNonBlock(mapPointsDiff)
                                } catch (e: IllegalArgumentException) {
                                    logger.warn("Received invalid message on channel $channel: $e\nmessage: $message")
                                }
                            }
                        }

                        RedisWrapper.subscribe(solidMapPointsSubscriber, "update:map:solid $robotId")
                    }
                }
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

    private suspend fun filterRobots() {
        val now = System.currentTimeMillis()
        val toRemove = robots.filter { it.value.first < now - 10000 }.keys
        RedisWrapper.use {
            for (robot in toRemove) {
                logger.info("Robot $robot disconnected (no keep-alive received for 10 seconds)")
                robots.remove(robot)

                srem("robots", robot)
            }
        }
    }
}
