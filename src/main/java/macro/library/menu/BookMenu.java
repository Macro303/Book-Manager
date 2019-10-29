package macro.library.menu;

import macro.library.book.Book;
import macro.library.book.Format;
import macro.library.book.Isbn;
import macro.library.console.Console;
import macro.library.database.BookTable;
import macro.library.open_library.OpenLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public abstract class BookMenu {
	private static final Logger LOGGER = LogManager.getLogger(BookMenu.class);

	@NotNull
	static Book loadBook(@NotNull Isbn isbn) {
		var book = OpenLibrary.searchBook(isbn);
		if (book == null) {
			LOGGER.warn("Unable to find book on Open Library under ISBN: " + isbn.getDisplay());
			book = enterBook(isbn);
		}
		book.setFormat(Format.selection());
		AuthorMenu.loadAuthor(isbn);
		return book.add();
	}

	@NotNull
	private static Book enterBook(@NotNull Isbn isbn) {
		var title = Console.displayPrompt("Title");
		var subtitle = Console.displayPrompt("Subtitle");
		if (subtitle.isBlank())
			subtitle = null;
		var publisher = Console.displayPrompt("Publisher");
		return new Book(isbn, title, subtitle, publisher, Format.PAPERBACK);
	}

	public static void editBook() {
		var isbn = Isbn.of(Console.displayPrompt("ISBN"));
		var book = BookTable.INSTANCE.selectUnique(isbn);
		if (book != null)
			book = loadBook(isbn);
		assert book != null;
		var title = Console.displayEdit("Title", book.getTitle());
		if (title != null)
			book.setTitle(title);
		var subtitle = Console.displayEdit("Subtitle", book.getSubtitle());
		if (subtitle != null)
			book.setSubtitle(subtitle);
		var publisher = Console.displayEdit("Publisher", book.getPublisher());
		if (publisher != null)
			book.setPublisher(publisher);
		book.setFormat(Format.selection());
		AuthorMenu.editAuthors(isbn);
		book.push();
	}
}