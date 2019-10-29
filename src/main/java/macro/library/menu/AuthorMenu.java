package macro.library.menu;

import macro.library.author.Author;
import macro.library.book.Isbn;
import macro.library.console.Console;
import macro.library.database.AuthorTable;
import macro.library.database.BookAuthorTable;
import macro.library.open_library.OpenLibrary;
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

	static void enterAuthor(@NotNull Isbn isbn) {
		var authors = BookAuthorTable.INSTANCE.searchBook(isbn);
		if (authors.isEmpty() || Console.displayAgreement("An Author already exists for this book, Add another Author")) {
			var authorNames = Console.displayPrompt("Enter Name (';' separated)").split(";");
			for (var authorName : authorNames) {
				var author = Author.parseName(authorName);
				var found = AuthorTable.INSTANCE.select(author.getFirstName(), author.getLastName());
				if (found != null)
					author.add();
				BookAuthorTable.INSTANCE.addBookAuthor(isbn, author.getUUID());
			}
		}
	}

	public static void editAuthors(@NotNull Isbn isbn){
		var authors = BookAuthorTable.INSTANCE.searchBook(isbn);
	}
}