package macro.library.config

import org.apache.logging.log4j.LogManager
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by Macro303 on 2019-Oct-30
 */
class Config {
	var proxy: Connection = Connection()
	var server: Connection = Connection("localhost", 6606)

	fun saveConfig(): Config {
		Files.newBufferedWriter(Paths.get("config.yaml")).use {
			it.write(YAML.dumpAsMap(this))
		}
		return this
	}

	companion object {
		private val LOGGER = LogManager.getLogger(Config::class.java)
		private val YAML: Yaml by lazy {
			val options = DumperOptions()
			options.defaultFlowStyle = DumperOptions.FlowStyle.FLOW
			options.isPrettyFlow = true
			Yaml(options)
		}
		var CONFIG: Config = loadConfig()

		fun loadConfig(): Config {
			val temp = File("config.yaml")
			if (!temp.exists())
				Config().saveConfig()
			return Files.newBufferedReader(Paths.get("config.yaml")).use {
				YAML.loadAs(it, Config::class.java)
			}.saveConfig()
		}
	}
}