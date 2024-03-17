package fr.bananasmoothii.limocontrolcenter.config

import fr.bananasmoothii.limocontrolcenter.isDockerized
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val redis: RedisConfig,
    val webserver: WebserverConfig,
) {
    companion object {
        fun load(): Config = loadConfig("server-config.yml")
    }
}

@Serializable
data class RedisConfig(
    val host: String,
    val port: Int,
    val user: String = "default",
    val password: String? = null,
    val database: Int = 0,
) {
    val hostOrDockerizedRedis: String
        get() = if (isDockerized) "redis" else host
}

@Serializable
data class WebserverConfig(
    val host: String,
    val port: Int,
)
