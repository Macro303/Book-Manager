package macro.library;

import macro.library.book.Book;
import macro.library.book.Format;
import macro.library.config.Config;
import macro.library.console.Console;
import macro.library.database.BookTable;
import macro.library.open_library.OpenLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.util.Arrays;

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
		}catch (FileNotFoundException fnfe){
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
		var options = new String[]{"List Books", "Add Book", "Load Book"};
		var selection = Console.displayMenu("Book Manager", options, "Exit");
		switch (selection) {
			case 0:
				return;
			case 1:
				listBooks();
				break;
			case 2:
				addBook();
				break;
			case 3:
				loadBook();
				break;
		}
		mainMenu();
	}

	private void listBooks() {
		var books = BookTable.INSTANCE.searchAll();
		Console.displayTable(books);
	}

	private void addBook() {
		Console.displaySubHeader("Add Book");
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var title = Console.displayPrompt("Title");
		var subtitle = Console.displayPrompt("Subtitle");
		if (subtitle.isBlank())
			subtitle = null;
		var author = Console.displayPrompt("Author");
		var publisher = Console.displayPrompt("Publisher");
		var options = Format.values();
		var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
		var format = options[selection - 1];
		var entry = BookTable.INSTANCE.selectUnique(isbn);
		if (entry == null)
			new Book(isbn, title, subtitle, author, publisher, format).add();
	}

	private void loadBook() {
		Console.displaySubHeader("Load Book");
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var book = OpenLibrary.searchBook(isbn);
		if (book != null) {
			var options = Format.values();
			var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
			var format = options[selection - 1];
			book.setFormat(format);
			book.push();
		}
	}
}
