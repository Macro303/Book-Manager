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
		var options = new String[]{"List", "Import"};
		var selection = Console.displayMenu("Collection", options);
		switch (selection) {
			case 0:
				return;
			case 1:
				listCollection();
				break;
			case 2:
				addToCollection();
				break;
			default:
				LOGGER.warn("Invalid Selection");
		}
		mainMenu();
	}

	private static void listCollection() {
		var books = new ArrayList<Book>();
		CollectionTable.INSTANCE.searchAll().forEach(book -> IntStream.range(0, book.getCount()).mapToObj(count -> book.getBook()).forEach(books::add));
		Collections.sort(books);
		Console.displayTable(books);
	}

	private static void addToCollection() {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var book = BookTable.INSTANCE.selectUnique(isbn);
		if (book == null)
			BookMenu.loadBook(isbn);
		var entry = CollectionTable.INSTANCE.selectUnique(isbn);
		if (entry != null) {
			if (Console.displayAgreement("This book already exists in your collection, Add Another Copy")) {
				entry.setCount(entry.getCount() + 1);
				entry.push();
			}
		} else
			new CollectionBook(isbn).add();
	}
}