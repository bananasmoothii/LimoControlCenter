package fr.bananasmoothii.limocontrolcenter.webserver

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/api") {
            call.respondText("Hello World!")
        }

        // static web page
        staticResources("/", "webstatic") {
            preCompressed(CompressedFileType.GZIP)
            enableAutoHeadResponse()
            default("index.html")
        }
    }
}