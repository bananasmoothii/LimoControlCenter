package fr.bananasmoothii

import fr.bananasmoothii.config.Config
import fr.bananasmoothii.redis.RedisWrapper
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager

val logger = LogManager.getLogger("general")!!.also {
    it.info("Starting the Limo Control Center...")
}

val config = Config.load()

fun main() {
    // add shutdown hook to close Redis
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            RedisWrapper.shutdown()
        }
    })

    runBlocking {
        RedisWrapper.use {
            set("test", "working!")
            logger.info("Redis test: ${get("test")}")
            del("test")
        }
    }
}

