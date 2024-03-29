package fr.bananasmoothii.limocontrolcenter.webserver

import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.redis.MapPoints
import fr.bananasmoothii.limocontrolcenter.redis.RedisWrapper
import fr.bananasmoothii.limocontrolcenter.robots.RobotManager
import fr.bananasmoothii.limocontrolcenter.robots.StringMapPointsDiff
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import redis.clients.jedis.Jedis

private const val maxMapPointDiffSize = 20000

/**
 * Translates two lists of points (added and removed) to a WebSocket frame.
 */
private suspend fun DefaultWebSocketServerSession.sendWebSocketMapPointDiff(mapPointsDiff: StringMapPointsDiff) {
    if (mapPointsDiff.size <= maxMapPointDiffSize) {
        send(Frame.Text(MapPoints.serializeMapPointsDiff(mapPointsDiff)))
        return
    }
    val chunks = mapPointsDiff.chunked(maxMapPointDiffSize) { chunk ->
        MapPoints.serializeMapPointsDiff(chunk.toSet())
    }
    for (chunk in chunks) {
        send(Frame.Text(chunk))
    }
}

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/api/test") {
            call.respondText("Hello World!")
        }

        webSocket("/ws/update-map") {
            RobotManager.updateMapSubscribers[this] = { _, mapPointsDiff ->
                sendWebSocketMapPointDiff(mapPointsDiff)
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val instruction = frame.readText()
                    if (instruction == "sendall") {
                        val all = RedisWrapper.use {
                            this.hgetAll("map")
                        }.mapTo(mutableSetOf()) { (key, value) ->
                            value + key // key is pos and value is type letter (W, U, P)
                        }
                        sendWebSocketMapPointDiff(all)
                    }
                }
            } finally {
                RobotManager.updateMapSubscribers.remove(this)
            }
        }

        webSocket("/ws/robot-pos") {
            RobotManager.updatePosSubscribers[this] = { robotId, newPos ->
                send(Frame.Text("$robotId $newPos"))
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val instruction = frame.readText()
                    if (instruction == "sendall") {
                        for (robot in RobotManager.robots.values) {
                            send(Frame.Text("${robot.id} ${robot.lastPosString}"))
                        }
                    }
                }
            } finally {
                RobotManager.updatePosSubscribers.remove(this)
            }
        }

        webSocket("/ws/update-goal") {
            val messagesSentByThisClient = mutableSetOf<String>()

            fun Jedis.sendGoalMsg(msg: String) {
                messagesSentByThisClient.add(msg)
                publish("update_goal", msg)
            }

            RobotManager.updateGoalSubscriber[this] = { robotId, goal ->
                val msgToSend = "$robotId ${RobotManager.serializeGoalUpdate(goal)}"
                if (msgToSend in messagesSentByThisClient) {
                    logger.debug("Not sending $msgToSend because it came from this client")
                    messagesSentByThisClient.remove(msgToSend)
                } else {
                    send(Frame.Text(msgToSend))
                }
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val instruction = frame.readText()
                    if (instruction == "sendall") {
                        RedisWrapper.use {
                            this.hgetAll("robots:goals")
                        }.forEach { (robotId, goalStr) ->
                            send(Frame.Text("$robotId $goalStr"))
                        }
                    } else {
                        // setting a goal
                        try {
                            val (robotId, goalStr) = instruction.split(' ', limit = 2)
                            val goal = RobotManager.deserializeGoalUpdate(goalStr)
                            if (goal == null) {
                                RedisWrapper.use {
                                    hdel("robots:goals", robotId)
                                    sendGoalMsg("$robotId remove")
                                }
                            } else {
                                RedisWrapper.use {
                                    logger.debug("Setting goal for {} to {}", robotId, goal)
                                    hset("robots:goals", robotId, goalStr)
                                    sendGoalMsg("$robotId $goalStr")
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Invalid instruction: $instruction", e)
                        }
                    }
                }
            } finally {
                RobotManager.updateGoalSubscriber.remove(this)
            }
        }

        // static web page
        staticResources("/", "webstatic") {
            preCompressed(CompressedFileType.GZIP)
            enableAutoHeadResponse()
            default("index.html") // catches all requests that don't match any other route
        }
    }
}