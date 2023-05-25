package github.buriedincode.bookshelf

import org.apache.logging.log4j.kotlin.Logging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class Settings private constructor(
    var databaseName: String = "bookshelf-java.sqlite",
    var websiteHost: String = "127.0.0.1",
    var websitePort: Int = 8003
) {
    fun save(): Settings {
        try {
            Files.newOutputStream(FILEPATH).use { stream ->
                val properties = Properties()
                properties.setProperty("database.name", databaseName)
                properties.setProperty("website.host", websiteHost)
                properties.setProperty("website.port", websitePort.toString())
                properties.store(stream, null)
            }
        } catch (ioe: IOException) {
            logger.error("Unable to save settings file", ioe)
        }
        return this
    }

    companion object : Logging {
        private val FILEPATH: Path = Paths.get(System.getProperty("user.home"), ".config", "bookshelf", "settings.properties")
        val INSTANCE: Settings by lazy {
            load()
        }

        private fun load(): Settings {
            val settings = Settings()
            if (!Files.exists(FILEPATH)) {
                settings.save()
            }
            try {
                Files.newInputStream(FILEPATH).use { stream ->
                    val properties = Properties()
                    properties.load(stream)
                    settings.databaseName = properties.getProperty("database.name")
                    settings.websiteHost = properties.getProperty("website.host")
                    settings.websitePort = properties.getProperty("website.port").toInt()
                    return settings.save()
                }
            } catch (ioe: IOException) {
                logger.error("Unable to load settings file", ioe)
            }
            return settings
        }
    }
}