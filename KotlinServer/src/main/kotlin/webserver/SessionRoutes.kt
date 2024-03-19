package fr.bananasmoothii.limocontrolcenter.webserver

import fr.bananasmoothii.limocontrolcenter.redis.DataSubscribers
import fr.bananasmoothii.limocontrolcenter.redis.RedisWrapper
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
private fun webSocketMapPointDiff(pointsToAdd: Collection<String>?, pointsToRemove: Collection<String>?): Frame {
    val builder = DataSubscribers.serializeMapPointsDiff(pointsToAdd, pointsToRemove)
    return Frame.Text(builder)
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

        webSocket("/ws/map/solid") {
            DataSubscribers.subscribeAllRobotsUpdateMapSolid(this) { mapPointsDiff ->
                send(webSocketMapPointDiff(mapPointsDiff.first, mapPointsDiff.second))
            }

            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val instruction = frame.readText()
                    if (instruction == "sendall") {
                        val all = RedisWrapper.use {
                            this.smembers("map:solid")
                        }
                        send(webSocketMapPointDiff(all, null))
                    }
                }
            } finally {
                DataSubscribers.unsubscribeAllRobotsUpdateMapSolid(this)
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