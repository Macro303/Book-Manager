package macro.library.external;

import macro.library.author.Author;
import macro.library.book.Book;
import macro.library.book.Isbn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Macro303 on 2019-Oct-30
 */
public abstract class Goodreads {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(OpenLibrary.class);
	@NotNull
	private static final String URL = "http://openlibrary.org/api/books";

	@Nullable
	public static Book searchBook(@NotNull Isbn isbn) {
		return null;
	}

	@NotNull
	public static List<Author> searchAuthors(@NotNull Isbn isbn) {
		var authorList = new ArrayList<Author>();
		return authorList;
	}
}