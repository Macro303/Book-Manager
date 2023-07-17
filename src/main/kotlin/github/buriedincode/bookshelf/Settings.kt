package github.buriedincode.bookshelf

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addPathSource
import com.sksamuel.hoplite.addResourceSource
import org.apache.logging.log4j.kotlin.Logging
import java.nio.file.Paths

enum class Environment {
    DEV,
    PROD
}

data class Database(val name: String)
data class Website(val host: String, val port: Int)
data class Settings(val env: Environment, val database: Database, val website: Website) {
    companion object : Logging {
        fun load(): Settings = ConfigLoaderBuilder.default()
            .addPathSource(
                Paths.get(System.getProperty("user.home"), ".config", "bookshelf", "settings.yaml"),
                optional = true,
                allowEmpty = true
            )
            .addPathSource(
                Paths.get(System.getProperty("user.home"), ".config", "bookshelf", "settings.properties"),
                optional = true,
                allowEmpty = true
            )
            .addResourceSource("/default.properties")
            .build()
            .loadConfigOrThrow<Settings>()
    }
}