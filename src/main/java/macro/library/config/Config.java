package macro.library.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public class Config {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(Config.class);
	@NotNull
	private static Yaml YAML;
	@NotNull
	public static Config CONFIG;
	@NotNull
	private Connection proxy;

	static {
		var options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
		options.setPrettyFlow(true);
		YAML = new Yaml(options);
	}

	public Config() {
		this(new Connection());
	}

	public Config(@NotNull Connection proxy) {
		this.proxy = proxy;
	}

	@NotNull
	public static Config loadConfig() throws FileNotFoundException {
		var temp = new File("config.yaml");
		if (!temp.exists())
			throw new FileNotFoundException("Unable to find `Config.yaml`");
		Config config;
		try (var br = Files.newBufferedReader(Paths.get("config.yaml"))) {
			config = YAML.loadAs(br, Config.class);
		} catch (IOException ioe) {
			LOGGER.error("Unable to load Config");
			return new Config();
		}
		config = config.saveConfig();
		return config;
	}

	@NotNull
	public Config saveConfig() {
		try (var bw = Files.newBufferedWriter(Paths.get("config.yaml"))) {
			bw.write(YAML.dumpAsMap(this));
		} catch (IOException ioe) {
			LOGGER.error("Unable to save Config");
		}
		return this;
	}

	@NotNull
	public Connection getProxy() {
		return proxy;
	}

	public void setProxy(@NotNull Connection proxy) {
		this.proxy = proxy;
	}
}