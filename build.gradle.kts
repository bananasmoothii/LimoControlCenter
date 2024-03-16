plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "fr.bananasmoothii"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Redis
    implementation("redis.clients:jedis:5.1.2")

    // logging
    implementation("org.apache.logging.log4j", "log4j-core", "2.23.1")
    implementation("org.apache.logging.log4j", "log4j-slf4j2-impl", "2.23.1")

    // YAML parsing
    implementation("com.charleskorn.kaml:kaml:0.57.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}