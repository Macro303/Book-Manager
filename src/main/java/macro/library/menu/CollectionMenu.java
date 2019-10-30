package macro.library.menu;

import macro.library.book.Book;
import macro.library.book.CollectionBook;
import macro.library.book.Isbn;
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
		var options = new String[]{"List", "Add", "Remove"};
		var selection = Console.displayMenu("Collection", options);
		try {
			switch (selection) {
				case 0:
					return;
				case 1:
					list();
					break;
				case 2:
					add();
					break;
				case 3:
					remove();
					break;
				default:
					LOGGER.warn("Invalid Selection");
			}
		} catch (IllegalArgumentException iae) {
			LOGGER.error(iae.getLocalizedMessage());
		}
		mainMenu();
	}

	private static void list() {
		var books = new ArrayList<Book>();
		CollectionTable.INSTANCE.searchAll().forEach(book -> IntStream.range(0, book.getCount()).mapToObj(count -> book.getBook()).forEach(books::add));
		Collections.sort(books);
		Console.displayTable(books);
	}

	private static void add() {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var book = BookTable.INSTANCE.selectUnique(isbn);
		if (book == null)
			book = BookMenu.loadBook(isbn);
		if (book == null)
			return;
		var entry = CollectionTable.INSTANCE.selectUnique(book.getISBN());
		if (entry == null)
			entry = new CollectionBook(book.getISBN(), 0).add();
		entry.setCount(entry.getCount() + 1);
		entry.push();
	}

	private static void remove() {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var book = BookTable.INSTANCE.selectUnique(isbn);
		if (book == null)
			return;
		var entry = CollectionTable.INSTANCE.selectUnique(book.getISBN());
		if (entry == null)
			return;
		if (entry.getCount() > 1) {
			entry.setCount(entry.getCount() - 1);
			entry.push();
		} else
			entry.remove();
	}
}