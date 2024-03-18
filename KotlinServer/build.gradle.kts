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
    mainClass.set("$group.MainKt")
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
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")

    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
}

kotlin {
    jvmToolchain(17)
}

tasks.register<Exec>("buildWeb") {
    inputs.files(project.fileTree("../vue-gui") {
        exclude("node_modules/**", "dist/**")
    })
    outputs.dir(project.file("../vue-gui/dist"))
    workingDir = File("../vue-gui")
    val npmCommand = "npm" + (if ("Windows" in System.getProperty("os.name")) ".cmd" else "")
    commandLine(npmCommand, "install")
    commandLine(npmCommand, "run", "build")
}

tasks.register<Copy>("buildAndCopyWeb") {
    dependsOn("buildWeb")
    val target = layout.buildDirectory.dir("resources/main/webstatic").get()
    doFirst {
        println("Copying files from vue-gui/dist to $target")
    }
    from("$projectDir/../vue-gui/dist")
    into(target)
}

tasks.shadowJar {
    mustRunAfter("buildAndCopyWeb")
    doFirst {
        val webStaticDir = layout.buildDirectory.dir("resources/main/webstatic").get()
        if (!webStaticDir.asFile.exists()) {
            throw IllegalStateException(
                "The webstatic directory doesn't exist. Run the buildAndCopyWeb task first, " +
                        "or manually compile the npm project and copy the files from dist/ to " +
                        webStaticDir.asFile.absolutePath
            )

        }
    }
}

tasks.register("runEverything") {
    dependsOn("buildAndCopyWeb", "runShadow")
}

// we are registering a new task so that the shadowJar task can still be run separately. This is useful in the Dockerfile
// where there is a container for npm and a container for gradle, and gradle doesn't have access to npm because
// everything is isolated.
tasks.register("webAndServerJar") {
    dependsOn("buildAndCopyWeb", "shadowJar")
}

tasks.test {
    mustRunAfter("buildAndCopyWeb")
    useJUnitPlatform()
}
