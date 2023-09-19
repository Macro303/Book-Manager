package github.buriedincode.bookshelf

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addPathSource
import com.sksamuel.hoplite.addResourceSource
import org.apache.logging.log4j.kotlin.Logging
import java.nio.file.Path
import kotlin.io.path.div

data class Settings(val environment: Environment, val database: Path, val website: Website) {
    enum class Environment {
        DEV,
        PROD,
    }

    data class Website(val host: String, val port: Int)

    companion object : Logging {
        fun load(): Settings =
            ConfigLoaderBuilder.default()
                .addPathSource(Utils.CONFIG_ROOT / "settings.yaml", optional = true, allowEmpty = true)
                .addPathSource(Utils.CONFIG_ROOT / "settings.json", optional = true, allowEmpty = true)
                .addPathSource(Utils.CONFIG_ROOT / "settings.conf", optional = true, allowEmpty = true)
                .addPathSource(Utils.CONFIG_ROOT / "settings.properties", optional = true, allowEmpty = true)
                .addResourceSource("/default.properties")
                .build()
                .loadConfigOrThrow<Settings>()
    }
}
