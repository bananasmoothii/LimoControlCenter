package fr.bananasmoothii.limocontrolcenter.webserver

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

/**
 * Translates two lists of points (added and removed) to a WebSocket frame.
 */
private fun webSocketMapPointDiff(mapPointsDiff: StringMapPointsDiff): Frame =
    Frame.Text(MapPoints.serializeMapPointsDiff(mapPointsDiff))

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
                send(webSocketMapPointDiff(mapPointsDiff))
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
                        send(webSocketMapPointDiff(all))
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

        webSocket("/ws/robot-goals") {
            RobotManager.updateGoalSubscriber[this] = { robotId, goal ->
                send(Frame.Text("$robotId ${8}")) // TODO
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val instruction = frame.readText()
                    if (instruction == "sendall") {
                        for (robot in RobotManager.robots.values) {
                            send(Frame.Text("${robot.id} ${8}"))
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