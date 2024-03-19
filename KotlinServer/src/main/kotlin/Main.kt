package fr.bananasmoothii.limocontrolcenter

import fr.bananasmoothii.limocontrolcenter.config.Config
import fr.bananasmoothii.limocontrolcenter.redis.DataSubscribers
import fr.bananasmoothii.limocontrolcenter.redis.RedisWrapper
import fr.bananasmoothii.limocontrolcenter.webserver.Webserver
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
                    val squareSize = 0.2
                    del("map:solid")
                    for (x in 0..10) {
                        if (x == 0 || x == 10) {
                            for (y in 0..10) {
                                sadd("map:solid", "${x * squareSize},${y * squareSize}")
                            }
                        } else {
                            sadd("map:solid", "${x * squareSize},0")
                            sadd("map:solid", "${x * squareSize},${10 * squareSize}")
                        }
                    }
                }
            } catch (e: JedisConnectionException) {
                logger.error("Redis is not working! Retrying in 5 seconds...")
                Thread.sleep(5000)
                continue
            }
            break
        }

        DataSubscribers.init()

        // this is blocking
        Webserver.start()
    }
}

