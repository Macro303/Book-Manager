package macro.library;

import macro.library.book.Book;
import macro.library.book.BookTable;
import macro.library.book.Format;
import macro.library.console.Colour;
import macro.library.console.Console;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by Macro303 on 2019-Oct-21
 */
class Library {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(Library.class);

	public Library() {
		LOGGER.info("Initializing Book Manager");
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
		var options = new String[]{"List Books", "Add Book"};
		var selection = Console.displayMenu$Book_Manager("Book Manager", options, "Exit");
		if (selection == 0)
			return;
		else if (selection == 1)
			listBooks();
		else if (selection == 2)
			addBook();
		mainMenu();
	}

	private void listBooks() {
		var books = BookTable.INSTANCE.searchAll();
		Console.displayTable$Book_Manager(books);
	}

	private void addBook() {
		Console.displaySubHeader$Book_Manager("Add Book");
		var isbn = Isbn.of(Console.displayPrompt$Book_Manager("ISBN").replace("-", ""));
		var name = Console.displayPrompt$Book_Manager("Name");
		var author = Console.displayPrompt$Book_Manager("Author");
		var series = Console.displayPrompt$Book_Manager("Series");
		var seriesNum = -1;
		try {
			seriesNum = Integer.parseInt(Console.displayPrompt$Book_Manager("Series #"));
		}catch (NumberFormatException ignored){}
		var options = Format.values();
		var selection = Console.displayMenu$Book_Manager("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
		var format = options[selection - 1];
		var entry = BookTable.INSTANCE.selectUnique(isbn);
		if(entry == null)
			new Book(isbn, name, author, series, seriesNum, format).add();
	}
}
