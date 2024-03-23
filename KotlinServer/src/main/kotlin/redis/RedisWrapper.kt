package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.config
import fr.bananasmoothii.limocontrolcenter.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.time.Duration

object RedisWrapper {
    private val pool: JedisPool

    private var redisSubscribersNb = 0

    init {
        val redis = config.redis
        logger.info("Connecting to Redis on ${redis.hostOrDockerizedRedis}:${redis.port}...")
        pool = JedisPool(
            redis.hostOrDockerizedRedis,
            redis.port,
            redis.user,
            redis.password
        )
        pool.setConfig(GenericObjectPoolConfig<Jedis?>().apply {
            maxTotal = 8
            setMaxWait(Duration.ofMillis(1000))
        })
    }

    /**
     * Use a Jedis instance from the pool, and close it after the block is executed.
     */
    suspend fun <T> use(block: Jedis.() -> T): T = withContext(Dispatchers.IO) {
        pool.resource.use { jedis ->
            block(jedis)
        }
    }

    /**
     * Subscribe to a Redis channel. Non-blocking.
     */
    fun subscribe(jedisPubSub: JedisPubSub, vararg channels: String) {
        val subscriber = object : Thread("RedisSub-${++redisSubscribersNb}") {
            override fun run() {
                pool.resource.use {
                    it.subscribe(jedisPubSub, *channels)
                }
            }
        }
        subscriber.start()
    }

    /**
     * Subscribe to a Redis channel. Non-blocking.
     */
    fun subscribe(vararg channels: String, callback: (channel: String, message: String) -> Unit) {
        subscribe(object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                callback(channel, message)
            }
        }, *channels)
    }

    fun shutdown() {
        logger.info("Shutting down Redis connection...")
        pool.close()
    }
}