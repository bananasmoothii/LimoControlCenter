package fr.bananasmoothii.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val redis: RedisConfig,
) {
    companion object {
        fun load(): Config = loadConfig("config.yml")
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
