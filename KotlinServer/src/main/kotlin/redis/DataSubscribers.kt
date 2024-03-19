package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.redis.Robot.Companion.filterRobots
import kotlinx.coroutines.*

typealias MapPointsDiff = Pair<Set<String>?, Set<String>?>

object DataSubscribers {

    /**
     * map of robotId to a pair of last keep-alive timestamp and JedisPubSubs
     */
    val robots = mutableMapOf<String, Robot>()

    // TODO: merge the maps from all robots in a coherent way
    private val allRobotsUpdateMapSolidSubscribers = mutableMapOf<Any, suspend (mapPointsDiff: MapPointsDiff) -> Unit>()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        ioScope.launch {
            while (true) {
                robots.filterRobots()
                delay(1000)
            }
        }
    }

    suspend fun init() {
        RedisWrapper.use {
            del("robots")
        }

        RedisWrapper.subscribe("general") { _, message ->
//            logger.debug("Received message on channel general: $message")
            val messageSplit = message.split(' ', limit = 2)
            val robotId = messageSplit[0]
            val command = messageSplit[1]
            val param = messageSplit.getOrNull(2)

            when (command) {
                "keep_alive" -> {

                    var robot = robots[robotId]
                    if (robot == null) {
                        logger.info("New robot connected: $robotId")

                        robot = Robot(robotId)
                        robots[robotId] = robot

                        robot.subscribeToUpdateMapSolid(this) { mapPointsDiff ->
                            allRobotsUpdateMapSolidSubscribers.values.forEach { subscriber ->
                                ioScope.launch {
                                    subscriber(mapPointsDiff)
                                }
                            }
                        }
                    }
                    robot.lastReceivedKeepAlive = System.currentTimeMillis()
                }
            }
        }
    }

    fun subscribeAllRobotsUpdateMapSolid(subscriber: Any, callback: suspend (MapPointsDiff) -> Unit) {
        allRobotsUpdateMapSolidSubscribers[subscriber] = callback
    }

    fun unsubscribeAllRobotsUpdateMapSolid(subscriber: Any) {
        allRobotsUpdateMapSolidSubscribers.remove(subscriber)
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
                builder.append(' ')
            }
        }
        builder.append('/')
        if (pointsToRemove != null) {
            for (point in pointsToRemove) {
                builder.append(point)
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
