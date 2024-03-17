package fr.bananasmoothii.limocontrolcenter

import fr.bananasmoothii.limocontrolcenter.config.Config
import fr.bananasmoothii.limocontrolcenter.redis.RedisWrapper
import fr.bananasmoothii.limocontrolcenter.webserver.Webserver
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

            // draw a square of points
            del("map:solid")
            for (x in 0..10) {
                if (x == 0 || x == 10) {
                    for (y in 0..10) {
                        sadd("map:solid", "$x,$y")
                    }
                } else {
                    sadd("map:solid", "$x,0")
                    sadd("map:solid", "$x,10")
                }
            }
        }

        // this is blocking
        Webserver.start()
    }
}

