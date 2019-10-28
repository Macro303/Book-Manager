package macro.library.menu;

import macro.library.Isbn;
import macro.library.book.Book;
import macro.library.book.Format;
import macro.library.book.WishlistBook;
import macro.library.console.Colour;
import macro.library.console.Console;
import macro.library.database.BookTable;
import macro.library.database.WishlistTable;
import macro.library.open_library.OpenLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

/**
 * Created by Macro303 on 2019-Oct-24
 */
public abstract class WishlistMenu {
	private static final Logger LOGGER = LogManager.getLogger(WishlistMenu.class);

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
					addToWishlist(false);
					break;
				case 3:
					addToWishlist(true);
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
		var books = new ArrayList<Book>();
		WishlistTable.INSTANCE.searchAll().forEach(book -> IntStream.range(0, book.getCount()).mapToObj(count -> book.getBook()).forEach(books::add));
		Collections.sort(books);
		Console.displayTable(books);
	}

	private static void addToWishlist(boolean load) throws IllegalArgumentException {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var book = BookTable.INSTANCE.selectUnique(isbn);
		if (book == null)
			BookMenu.addBook(isbn, load);
		else if (Console.displayAgreement("A Book with that ISBN already exists, Update Entry"))
			BookMenu.updateBook(isbn, load);
		var entry = WishlistTable.INSTANCE.selectUnique(isbn);
		if (entry != null) {
			if (Console.displayAgreement("A Book with that ISBN already exists in your wishlist, Add another copy")) {
				entry.setCount(entry.getCount() + 1);
				entry.push();
			}
		} else
			new WishlistBook(isbn).add();
	}
}