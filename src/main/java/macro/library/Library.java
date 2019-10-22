package macro.library;

import macro.library.book.Book;
import macro.library.book.Format;
import macro.library.config.Config;
import macro.library.console.Colour;
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
		var options = new String[]{"List Books", "Add Book", "Load Book", "Reload Book"};
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
			case 4:
				reloadBook();
				break;
		}
		mainMenu();
	}

	private void listBooks() {
		var books = BookTable.INSTANCE.searchAll();
		Console.displayTable(books);
	}

	private void reloadBook(@NotNull Isbn ISBN) {
		var book = OpenLibrary.searchBook(ISBN);
		if (book != null) {
			var options = Format.values();
			var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
			var format = options[selection - 1];
			book.setFormat(format);
			book.push();
		} else
			Console.display("Unable to find Book on Open Library", Colour.YELLOW);
	}

	private void reloadBook() {
		Console.displaySubHeader("Load Book");
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var entry = BookTable.INSTANCE.selectUnique(isbn);
		if (entry == null) {
			Console.display("Unable to find Book", Colour.YELLOW);
			if (Console.displayAgreement("Load Book"))
				loadBook(isbn);
		} else
			reloadBook(isbn);
	}

	private void loadBook(@NotNull Isbn ISBN) {
		var book = OpenLibrary.searchBook(ISBN);
		if (book != null) {
			var options = Format.values();
			var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
			var format = options[selection - 1];
			book.setFormat(format);
			book.add();
		} else {
			Console.display("Unable to find Book on Open Library", Colour.YELLOW);
			if (Console.displayAgreement("Add Manually"))
				addBook(ISBN);
		}
	}

	private void loadBook() {
		Console.displaySubHeader("Load Book");
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var entry = BookTable.INSTANCE.selectUnique(isbn);
		if (entry == null)
			loadBook(isbn);
		else {
			Console.display("A Book with that ISBN already exists", Colour.YELLOW);
			if (Console.displayAgreement("Reload Book"))
				reloadBook(isbn);
		}
	}

	private void addBook(@NotNull Isbn ISBN) {
		var title = Console.displayPrompt("Title");
		var subtitle = Console.displayPrompt("Subtitle");
		if (subtitle.isBlank())
			subtitle = null;
		var author = Console.displayPrompt("Author");
		var publisher = Console.displayPrompt("Publisher");
		var options = Format.values();
		var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
		var format = options[selection - 1];
		new Book(ISBN, title, subtitle, author, publisher, format).add();
	}

	private void addBook() {
		Console.displaySubHeader("Add Book");
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var entry = BookTable.INSTANCE.selectUnique(isbn);
		if (entry == null)
			addBook(isbn);
		else {
			Console.display("A Book with that ISBN already exists", Colour.YELLOW);
			if (Console.displayAgreement("Reload Book"))
				reloadBook(isbn);
		}
	}
}
