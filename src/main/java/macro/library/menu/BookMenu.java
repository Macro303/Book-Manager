package macro.library.menu;

import macro.library.book.Book;
import macro.library.book.Format;
import macro.library.book.Isbn;
import macro.library.external.OpenLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public abstract class BookMenu {
	private static final Logger LOGGER = LogManager.getLogger(BookMenu.class);

	@Nullable
	static Book loadBook(@NotNull Isbn isbn) {
		var book = OpenLibrary.searchBook(isbn);
		if (book == null) {
			LOGGER.warn("Unable to find book on Open Library under ISBN: " + isbn.getDisplay());
			return null;
		}
		book.setFormat(Format.selection());
		AuthorMenu.loadAuthor(isbn);
		return book.add();
	}
}