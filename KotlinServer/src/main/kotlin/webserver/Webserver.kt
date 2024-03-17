package fr.bananasmoothii.limocontrolcenter.webserver

import fr.bananasmoothii.limocontrolcenter.config
import fr.bananasmoothii.limocontrolcenter.logger
import fr.bananasmoothii.limocontrolcenter.webserver.plugins.configureHTTP
import fr.bananasmoothii.limocontrolcenter.webserver.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

object Webserver {
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
    configureSerialization()
    configureRouting()
}