package fr.bananasmoothii.limocontrolcenter.webserver

import fr.bananasmoothii.limocontrolcenter.config
import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.webserver.plugins.configureHTTP
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object Webserver {
    /**
     * Start the webserver. Blocking.
     */
    fun start() {
        val host = config.webserver.host
        val port = config.webserver.port
        logger.info("Starting the webserver on http://$host${if (port == 80) "" else ":$port"}")
        embeddedServer(
            Netty,
            host = host,
            port = port,
            module = Application::module
        ).start(wait = true)
    }
}

private fun Application.module() {
    configureHTTP()

    install(ContentNegotiation) {
        json()
    }

    install(WebSockets)

    install(CallLogging) {
        level = Level.DEBUG
        this.logger = LoggerFactory.getLogger("call-log")!!
    }

    configureRouting()
}