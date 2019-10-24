package macro.library;

import macro.library.book.Book;
import macro.library.book.Format;
import macro.library.console.Colour;
import macro.library.console.Console;
import macro.library.database.BookTable;
import macro.library.open_library.OpenLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * Created by Macro303 on 2019-Oct-24
 */
public abstract class Wishlist {
	private static final Logger LOGGER = LogManager.getLogger(Wishlist.class);

	public static void mainMenu() {
		var options = new String[]{"List", "Add", "Import"};
		var selection = Console.displayMenu("Wishlist", options);
		try {
			switch (selection) {
				case 0:
					return;
				case 1:
					listWishlist();
					break;
				case 2:
					addToWishlist();
					break;
				case 3:
					importToWishlist();
					break;
				default:
					LOGGER.warn("Invalid Selection");
			}
		}catch (IllegalArgumentException iae){
			LOGGER.warn("Invalid Field", iae);
		}
		mainMenu();
	}

	private static void listWishlist() {
		var books = BookTable.INSTANCE.searchAll();
		Console.displayTable(books);
	}

	private static void addToWishlist() throws IllegalArgumentException {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var entry = BookTable.INSTANCE.selectUnique(isbn);
		var isNew = true;
		if (entry != null)
			if (Console.displayAgreement("A Book with that ISBN already exists, Update Book"))
				isNew = false;
			else
				return;
		var title = Console.displayPrompt("Title");
		var subtitle = Console.displayPrompt("Subtitle");
		if (subtitle.isBlank())
			subtitle = null;
		var author = Console.displayPrompt("Author");
		var publisher = Console.displayPrompt("Publisher");
		var options = Format.values();
		var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
		var format = options[selection - 1];
		entry = new Book(isbn, title, subtitle, author, publisher, format);
		if (isNew)
			entry.add();
		else
			entry.push();
	}

	private static void importToWishlist() throws IllegalArgumentException {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var entry = BookTable.INSTANCE.selectUnique(isbn);
		var isNew = true;
		if (entry != null)
			if (Console.displayAgreement("A Book with that ISBN already exists, Update Book"))
				isNew = false;
			else
				return;
		entry = OpenLibrary.searchBook(isbn);
		if (entry == null)
			Console.display("Unable to find Book on Open Library", Colour.YELLOW);
		else {
			var options = Format.values();
			var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
			var format = options[selection - 1];
			entry.setFormat(format);
			if (isNew)
				entry.add();
			else
				entry.push();
		}
	}
}