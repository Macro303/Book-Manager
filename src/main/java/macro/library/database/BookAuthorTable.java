package macro.library.database;

import macro.library.author.Author;
import macro.library.book.Book;
import macro.library.book.Isbn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public class BookAuthorTable extends Table<String> {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(BookTable.class);
	@NotNull
	public static final BookAuthorTable INSTANCE = new BookAuthorTable();

	private BookAuthorTable() {
		super("bookAuthor");
	}

	@Override
	protected void createTable() {
		var query = String.format("CREATE TABLE %s(bookId TEXT NOT NULL, authorId TEXT NOT NULL, PRIMARY KEY(bookId, authorId), UNIQUE(bookId, authorId));", tableName);
		insert(query);
	}

	@Nullable
	@Override
	protected String parse(@NotNull ResultSet result) throws SQLException {
		return result.getString(1);
	}

	public List<Book> searchAuthor(@NotNull UUID authorId) {
		var query = String.format("SELECT bookId FROM %s WHERE authorId = ?;", tableName);
		return search(query, authorId).stream().map(it -> BookTable.INSTANCE.selectUnique(Isbn.of(it))).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public List<Author> searchBook(@NotNull Isbn bookId) {
		var query = String.format("SELECT authorId FROM %s WHERE bookId = ?;", tableName);
		return search(query, bookId).stream().map(it -> AuthorTable.INSTANCE.selectUnique(UUID.fromString(it))).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public boolean addBookAuthor(@NotNull Isbn bookId, @NotNull UUID authorId) {
		var query = String.format("INSERT INTO %s(bookId, authorId) VALUES(?, ?)", tableName);
		return insert(query, bookId, authorId);
	}
}