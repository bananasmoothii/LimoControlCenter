package fr.bananasmoothii.limocontrolcenter.webserver

import fr.bananasmoothii.limocontrolcenter.redis.DataSubscribers
import fr.bananasmoothii.limocontrolcenter.redis.RedisWrapper
import fr.bananasmoothii.limocontrolcenter.redis.StringMapPointsDiff
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
    Frame.Text(DataSubscribers.serializeMapPointsDiff(mapPointsDiff))

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
            DataSubscribers.subscribeAllRobotsUpdateMapSolid(this) { mapPointsDiff ->
                send(webSocketMapPointDiff(mapPointsDiff))
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val instruction = frame.readText()
                    if (instruction == "sendall") {
                        val all = RedisWrapper.use {
                            this.hgetAll("map:solid")
                        }.mapTo(mutableSetOf()) { (key, value) ->
                            value + key // key is pos and value is type letter (W, U, P)
                        }
                        send(webSocketMapPointDiff(all))
                    }
                }
            } finally {
                DataSubscribers.unsubscribeAllRobotsUpdateMapSolid(this)
            }
        }

        webSocket("/ws/robot-pos") {
            DataSubscribers.subscribeAllRobotsUpdatePos(this) { robotId, newPos ->
                send(Frame.Text("$robotId $newPos"))
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val instruction = frame.readText()
                    if (instruction == "sendall") {
                        for (robot in DataSubscribers.robots.values) {
                            send(Frame.Text("${robot.id} ${robot.lastPosString}"))
                        }
                    }
                }
            } finally {
                DataSubscribers.unsubscribeAllRobotsUpdatePos(this)
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