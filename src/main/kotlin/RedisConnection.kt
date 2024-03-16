package fr.bananasmoothii

import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.github.crackthecodeabhi.kreds.connection.newSubscriberClient
import io.github.crackthecodeabhi.kreds.connection.shutdown
import io.github.crackthecodeabhi.kreds.protocol.KredsRedisDataException
import kotlinx.coroutines.CoroutineScope
import kotlin.time.measureTime

suspend fun connectRedis() {
    newClient(Endpoint(config.redis.host, config.redis.port)).use { client ->
        try {
            config.redis.password?.let { client.auth(it) }
        } catch (e: KredsRedisDataException) {
            logger.error("Failed to authenticate to Redis, most likely due to incorrect password", e)
            shutdownRedis()
            return
        }

        client.select(config.redis.database.toULong())

        logger.info(client.get("test1"))
    }
}

suspend fun shutdownRedis() {
    logger.info("Shutting down...")
    val shutdownTime = measureTime {
        shutdown() // shutdown the Kreds Event loop.
    }
    logger.info("Shutdown complete in $shutdownTime")
}