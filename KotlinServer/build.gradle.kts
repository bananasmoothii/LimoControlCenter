val ktor_version: String by project

plugins {
    kotlin("jvm") version "1.9.23"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "1.9.23"
    id("io.ktor.plugin") version "3.0.0-beta-1"
}

group = "fr.bananasmoothii.limocontrolcenter"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("fr.bananasmoothii.limocontrolcenter.MainKt")
}

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

    // webserver
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-compression-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")

    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}