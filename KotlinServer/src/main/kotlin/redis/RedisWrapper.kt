package fr.bananasmoothii.limocontrolcenter.redis

import fr.bananasmoothii.limocontrolcenter.config
import fr.bananasmoothii.limocontrolcenter.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

object RedisWrapper {
    private val pool = JedisPool(config.redis.host, config.redis.port, config.redis.user, config.redis.password)

    private var redisSubscribersNb = 0

    /**
     * Use a Jedis instance from the pool, and close it after the block is executed.
     */
    suspend fun use(block: Jedis.() -> Unit) = withContext(Dispatchers.IO) {
        pool.resource.use { jedis ->
            block(jedis)
        }
    }

    /**
     * Subscribe to a Redis channel. Non-blocking.
     */
    fun subscribe(vararg channels: String, jedisPubSub: JedisPubSub) {
        val subscriber = object : Thread("RedisSub-${++redisSubscribersNb}") {
            override fun run() {
                val jedis = pool.resource
                jedis.subscribe(jedisPubSub, *channels)
            }
        }
        subscriber.start()
    }

    /**
     * Subscribe to a Redis channel. Non-blocking.
     */
    fun subscribe(vararg channels: String, callback: (channel: String, message: String) -> Unit) {
        subscribe(*channels, jedisPubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                callback(channel, message)
            }
        })
    }

    fun shutdown() {
        logger.info("Shutting down Redis connection...")
        pool.close()
    }
}