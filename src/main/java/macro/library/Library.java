package macro.library;

import macro.library.config.Config;
import macro.library.console.Console;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;

/**
 * Created by Macro303 on 2019-Oct-21
 */
class Library {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(Library.class);

	public Library() {
		LOGGER.info("Initializing Book Manager");
		try {
			Config.CONFIG = Config.loadConfig();
		} catch (FileNotFoundException fnfe) {
			Config.CONFIG = new Config().saveConfig();
		}
		mainMenu();
	}

	public static void main(@Nullable String... args) {
		checkLogLevels();
		new Library();
	}

	private static void checkLogLevels() {
		LOGGER.trace("TRACE | is Visible");
		LOGGER.debug("DEBUG | is Visible");
		LOGGER.info("INFO  | is Visible");
		LOGGER.warn("WARN  | is Visible");
		LOGGER.error("ERROR | is Visible");
		LOGGER.fatal("FATAL | is Visible");
	}

	private void mainMenu() {
		var options = new String[]{"Collection", "Wishlist"};
		var selection = Console.displayMenu("Book Manager", options, "Exit");
		switch (selection) {
			case 0:
				return;
			case 1:
				Collection.mainMenu();
				break;
			case 2:
//				Wishlist.mainMenu();
				LOGGER.warn("Wishlist Not Yet Implemented");
				break;
			default:
				LOGGER.warn("Invalid Selection");
		}
		mainMenu();
	}
}