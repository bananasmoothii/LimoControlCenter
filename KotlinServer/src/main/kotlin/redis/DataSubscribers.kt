package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.redis.Robot.Companion.filterRobots
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

typealias StringMapPointsDiff = Set<String>

object DataSubscribers {

    /**
     * map of robotId to a pair of last keep-alive timestamp and JedisPubSubs
     */
    val robots = mutableMapOf<String, Robot>()

    // TODO: merge the maps from all robots in a coherent way
    private val allRobotsUpdateMapSolidSubscribers =
        ConcurrentHashMap<Any, suspend (mapPointsDiff: StringMapPointsDiff) -> Unit>()

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

    fun subscribeAllRobotsUpdateMapSolid(subscriber: Any, callback: suspend (StringMapPointsDiff) -> Unit) {
        allRobotsUpdateMapSolidSubscribers[subscriber] = callback
    }

    fun unsubscribeAllRobotsUpdateMapSolid(subscriber: Any) {
        allRobotsUpdateMapSolidSubscribers.remove(subscriber)
    }

    fun serializeMapPointsDiff(
        mapPointsDiff: StringMapPointsDiff,
    ): String {
        return mapPointsDiff.joinToString(" ")
    }

    fun deserializeMapPointsDiff(serialized: String): StringMapPointsDiff {
        val mapPointDiff = serialized.splitToSequence(' ').toSet()
        return mapPointDiff
    }
}
