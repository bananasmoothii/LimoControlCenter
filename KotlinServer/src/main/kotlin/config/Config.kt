package fr.bananasmoothii.limocontrolcenter.config

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
)

@Serializable
data class WebserverConfig(
    val host: String,
    val port: Int,
)
