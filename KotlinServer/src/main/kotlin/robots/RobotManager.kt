package fr.bananasmoothii.limocontrolcenter.robots

import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.redis.MapPoints
import fr.bananasmoothii.limocontrolcenter.redis.MapPoints.launchSaveRoundedMapPointDiff
import fr.bananasmoothii.limocontrolcenter.redis.RedisWrapper
import fr.bananasmoothii.limocontrolcenter.threeD.Point2D
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

typealias StringMapPointsDiff = Set<String>

object RobotManager {

    val robots = mutableMapOf<String, Robot>()

    val updateMapSubscribers =
        ConcurrentHashMap<Any, suspend (robotId: String, mapPointsDiff: StringMapPointsDiff) -> Unit>()

    val updatePosSubscribers = ConcurrentHashMap<Any, suspend (robotId: String, newPos: String) -> Unit>()

    val updateGoalSubscriber = ConcurrentHashMap<Any, suspend (robotId: String, newGoal: Point2D?) -> Unit>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun init() {
        RedisWrapper.use {
            del("robots")
        }

        scope.launch {
            while (true) {
                robots.filterRobots()
                delay(1000)
            }
        }.start()


        RedisWrapper.subscribe("general") { _, message ->
            val messageSplit = message.split(' ', limit = 2)
            val robotId = messageSplit[0]
            val command = messageSplit[1]
            // add parameters as third element if needed

            when (command) {
                "keep_alive" -> {

                    var robot = robots[robotId]
                    if (robot == null) {
                        logger.info("New robot connected: $robotId")

                        robot = Robot(robotId)
                        robots[robotId] = robot
                    }
                    robot.lastReceivedKeepAlive = System.currentTimeMillis()
                }
            }
        }

        RedisWrapper.subscribe("update_pos") { _, message ->
            val messageSplit = message.split(' ', limit = 2)
            val robotId = messageSplit[0]
            val newPos = messageSplit[1]

            updatePosSubscribers.forEach { (_, subscriber) ->
                scope.launch {
                    subscriber(robotId, newPos)
                }
            }

            scope.launch {
                RedisWrapper.use {
                    hset("robots:pos", robotId, newPos)
                }
            }
        }

        RedisWrapper.subscribe("update_map") { _, message ->
            try {
                val messageSplit = message.split(' ', limit = 2)
                val robotId = messageSplit[0]
                val mapPointsDiff = MapPoints.deserializeAndRoundMapPointsDiff(messageSplit[1])

                updateMapSubscribers.forEach { (_, subscriber) ->
                    scope.launch {
                        subscriber(robotId, mapPointsDiff)
                    }
                }

                scope.launchSaveRoundedMapPointDiff(mapPointsDiff)
            } catch (e: IllegalArgumentException) {
                logger.error("Invalid update_map message: $message", e)
            }
        }

        RedisWrapper.subscribe("update_goal") { _, message ->
            val messageSplit = message.split(' ', limit = 2)
            val robotId = messageSplit[0]
            val newGoalStr = messageSplit[1]

            val newGoal = deserializeGoalUpdate(newGoalStr)

            updateGoalSubscriber.forEach { (_, subscriber) ->
                scope.launch {
                    subscriber(robotId, newGoal)
                }
            }

            scope.launch {
                RedisWrapper.use {
                    if (newGoal == null) {
                        hdel("robots:goals", robotId)
                    } else {
                        hset("robots:goals", robotId, "${newGoal.x},${newGoal.y}")
                    }
                }
            }
        }
    }

    fun deserializeGoalUpdate(message: String): Point2D? =
        if (message == "remove") null else message.split(',', limit = 2).let {
            Point2D(it[0].toDouble(), it[1].toDouble())
        }

    fun serializeGoalUpdate(goal: Point2D?): String =
        if (goal == null) "remove" else "${goal.x},${goal.y}"


    private const val KEEP_ALIVE_TIMEOUT: Long = 1 * 1000L

    /**
     * Remove robots that didn't send a keep-alive in the last [KEEP_ALIVE_TIMEOUT] milliseconds.
     */
    private suspend fun MutableMap<String, Robot>.filterRobots() {
        val now = System.currentTimeMillis()
        val iterator = this.iterator()
        while (iterator.hasNext()) {
            val robot = iterator.next().value
            if (now - robot.lastReceivedKeepAlive > KEEP_ALIVE_TIMEOUT) {
                logger.info("Robot ${robot.id} disconnected (no keep-alive received for 1 second)")

                updatePosSubscribers.forEach { (_, subscriber) ->
                    scope.launch {
                        subscriber(robot.id, "remove")
                    }
                }

                RedisWrapper.use {
                    hdel("robots:pos", robot.id)
                    hdel("robots:goals", robot.id)
                }

                iterator.remove()
            }
        }
    }
}