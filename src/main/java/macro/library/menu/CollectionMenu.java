package macro.library.menu;

import macro.library.Isbn;
import macro.library.book.Book;
import macro.library.book.CollectionBook;
import macro.library.console.Console;
import macro.library.database.BookTable;
import macro.library.database.CollectionTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

/**
 * Created by Macro303 on 2019-Oct-24
 */
public abstract class CollectionMenu {
	private static final Logger LOGGER = LogManager.getLogger(CollectionMenu.class);

	public static void mainMenu() {
		var options = new String[]{"List", "Add", "Import"};
		var selection = Console.displayMenu("Collection", options);
		try {
			switch (selection) {
				case 0:
					return;
				case 1:
					listCollection();
					break;
				case 2:
					addToCollection(false);
					break;
				case 3:
					addToCollection(true);
					break;
				default:
					LOGGER.warn("Invalid Selection");
			}
		} catch (IllegalArgumentException iae) {
			LOGGER.warn("Invalid Field", iae);
		}
		mainMenu();
	}

	private static void listCollection() {
		var books = new ArrayList<Book>();
		CollectionTable.INSTANCE.searchAll().forEach(book -> IntStream.range(0, book.getCount()).mapToObj(count -> book.getBook()).forEach(books::add));
		Collections.sort(books);
		Console.displayTable(books);
	}

	private static void addToCollection(boolean load) throws IllegalArgumentException {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var book = BookTable.INSTANCE.selectUnique(isbn);
		if (book == null)
			BookMenu.addBook(isbn, load);
		else if (Console.displayAgreement("A Book with that ISBN already exists, Update Entry"))
			BookMenu.updateBook(isbn, load);
		var entry = CollectionTable.INSTANCE.selectUnique(isbn);
		if (entry != null) {
			if (Console.displayAgreement("A Book with that ISBN already exists in your collection, Increase Count")) {
				entry.setCount(entry.getCount() + 1);
				entry.push();
			}
		} else
			new CollectionBook(isbn).add();
	}
}