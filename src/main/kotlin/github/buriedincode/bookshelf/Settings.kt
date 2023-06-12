package github.buriedincode.bookshelf

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.properties.toProperties
import com.uchuhimo.konf.source.toml
import com.uchuhimo.konf.source.toml.toToml
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.source.yaml.toYaml
import org.apache.logging.log4j.kotlin.Logging
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.exists

enum class Environment {
    DEV,
    PROD
}

object Settings: ConfigSpec(prefix = ""), Logging {
    private val PROPERTIES_PATH: Path = Paths.get(System.getProperty("user.home"), ".config", "bookshelf", "settings.properties")
    private val YAML_PATH: Path = Paths.get(System.getProperty("user.home"), ".config", "bookshelf", "settings.yaml")

    val env by optional(default = Environment.DEV)
    object Database: ConfigSpec(), Logging {
        val name by optional(default = "bookshelf.sqlite")
    }
    object Website: ConfigSpec(), Logging {
        val host by optional(default = "127.0.0.1")
        val port by optional(default = 25711)
    }

    fun loadSettings(): Config = Config{
        addSpec(Settings)
    }.from.properties.file(file = PROPERTIES_PATH.toFile(), optional = true)
        .from.yaml.file(file = YAML_PATH.toFile(), optional = true)
        .from.systemProperties()

    fun saveSettings(config: Config = loadSettings()) {
        if(YAML_PATH.exists())
            config.toYaml.toFile(file = YAML_PATH.toFile())
        else
            config.toProperties.toFile(PROPERTIES_PATH.toFile())
    }
}