package fr.bananasmoothii.limocontrolcenter

import fr.bananasmoothii.limocontrolcenter.config.Config
import fr.bananasmoothii.limocontrolcenter.redis.DataSubscribers
import fr.bananasmoothii.limocontrolcenter.redis.MapPoints.CUBE_SIZE
import fr.bananasmoothii.limocontrolcenter.redis.RedisWrapper
import fr.bananasmoothii.limocontrolcenter.webserver.Webserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import redis.clients.jedis.exceptions.JedisConnectionException

val logger = LogManager.getLogger("general")!!.also {
    it.info("Starting the Limo Control Center...")
}

val config = Config.load()

var isDockerized = false

fun main(args: Array<String>) {
    isDockerized = args.contains("--dockerized")

    // add shutdown hook to close Redis
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            RedisWrapper.shutdown()
        }
    })

    runBlocking {
        while (true) {
            try {
                RedisWrapper.use {
                    // test Redis
                    set("limo control center test", "working!")
                    val test = get("limo control center test") ?: error("Redis is not working!")
                    logger.info("Redis test: $test")
                    del("limo control center test")

                    // draw a square of points // TODO: remove this
                    del("map:solid")
                    for (x in -10..10) {
                        if (x == -10 || x == 10) {
                            for (y in -10..10) {
                                hset("map:solid", "${x * CUBE_SIZE},${y * CUBE_SIZE}", "W")
                            }
                        } else {
                            hset("map:solid", "${x * CUBE_SIZE},${-10 * CUBE_SIZE}", "W")
                            hset("map:solid", "${x * CUBE_SIZE},${10 * CUBE_SIZE}", "W")
                        }
                    }
                }
            } catch (e: JedisConnectionException) {
                logger.error("Redis is not working! Retrying in 5 seconds...")
                delay(5000)
                continue
            }
            break
        }

        DataSubscribers.init()

        // this is blocking
        Webserver.start()
    }
}

