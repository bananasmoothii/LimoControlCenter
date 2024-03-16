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
            // test Redis
            set("limo control center test", "working!")
            val test = get("limo control center test") ?: error("Redis is not working!")
            logger.info("Redis test: $test")
            del("limo control center test")
        }
    }
}

