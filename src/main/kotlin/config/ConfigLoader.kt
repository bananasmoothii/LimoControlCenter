package fr.bananasmoothii.config

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import fr.bananasmoothii.logger
import kotlinx.serialization.serializer
import java.io.File

val yaml = Yaml(configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property))

/**
 * Load a config file from the plugin's data folder, or creates it first if it doesn't exist.
 * If the system property `resetConfig` is set to `true`, the file will be deleted and recreated from the resources.
 */
inline fun <reified T> loadConfig(fileName: String): T {
    val file = File(fileName)
    if (file.exists()) {
        if (! System.getProperty("resetConfig", "false").toBoolean()) {
            // file exists and we are allowed to use it
            return yaml.decodeFromStream(serializer<T>(), file.inputStream())
        } else {
            logger.info("Resetting config file $fileName (property resetConfig is true)")
            file.delete()
        }
    }
    // we need to create the file, so we copy it from resources
    logger.info("Generating config file $fileName")
    file.parentFile?.mkdirs()
    file.createNewFile()
    val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)
        ?: throw IllegalArgumentException("Resource $fileName not found")
    val fileOutputStream = file.outputStream()
    resourceStream.copyTo(fileOutputStream)
    fileOutputStream.close()
    resourceStream.close()
    return yaml.decodeFromStream(file.inputStream())
}

/**
 * Load a config file from the resources and does not create it as an actual file.
 */
inline fun <reified T> loadConfigFromResources(fileName: String): T {
    val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)
        ?: throw IllegalArgumentException("Resource $fileName not found")
    return yaml.decodeFromStream(resourceStream)
}
