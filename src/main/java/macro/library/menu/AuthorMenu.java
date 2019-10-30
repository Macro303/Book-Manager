package macro.library.menu;

import macro.library.book.Isbn;
import macro.library.database.BookAuthorTable;
import macro.library.external.OpenLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Macro303 on 2019-Oct-29
 */
abstract class AuthorMenu {
	private static final Logger LOGGER = LogManager.getLogger(AuthorMenu.class);

	static void loadAuthor(@NotNull Isbn isbn) {
		var authors = OpenLibrary.searchAuthors(isbn);
		var bookAuthors = BookAuthorTable.INSTANCE.searchBook(isbn);
		authors.removeAll(bookAuthors);
		for (var author : authors) {
			BookAuthorTable.INSTANCE.addBookAuthor(isbn, author.getUUID());
		}
	}
}