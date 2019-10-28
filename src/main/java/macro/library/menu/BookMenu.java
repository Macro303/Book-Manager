package macro.library.menu;

import macro.library.Isbn;
import macro.library.book.Book;
import macro.library.book.Format;
import macro.library.console.Console;
import macro.library.open_library.OpenLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Created by Macro303 on 2019-Oct-29
 */
abstract class BookMenu {
	private static final Logger LOGGER = LogManager.getLogger(BookMenu.class);

	@NotNull
	private static Book enterDetails(@NotNull Isbn isbn){
		var title = Console.displayPrompt("Title");
		var subtitle = Console.displayPrompt("Subtitle");
		if (subtitle.isBlank())
			subtitle = null;
		var author = Console.displayPrompt("Author");
		var publisher = Console.displayPrompt("Publisher");
		var options = Format.values();
		var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
		var format = options[selection - 1];
		return new Book(isbn, title, subtitle, author, publisher, format);
	}

	@NotNull
	private static Book loadDetails(@NotNull Isbn isbn){
		var book = OpenLibrary.searchBook(isbn);
		if (book == null){
			LOGGER.warn("Unable to find book on Open Library under ISBN: " + isbn.getDisplay());
			return enterDetails(isbn);
		}
		var options = Format.values();
		var selection = Console.displayMenu("Format", Arrays.stream(options).map(Format::getDisplay).toArray(String[]::new), null);
		var format = options[selection - 1];
		book.setFormat(format);
		return book;
	}

	static void addBook(@NotNull Isbn isbn, boolean load){
		Book book;
		if(load)
			book = loadDetails(isbn);
		else
			book = enterDetails(isbn);
		book.add();
	}

	static void updateBook(@NotNull Isbn isbn, boolean load){
		Book book;
		if(load)
			book = loadDetails(isbn);
		else
			book = enterDetails(isbn);
		book.push();
	}
}