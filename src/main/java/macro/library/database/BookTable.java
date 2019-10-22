package macro.library.database;

import macro.library.Isbn;
import macro.library.book.Book;
import macro.library.book.Format;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Macro303 on 2019-Oct-22
 */
public class BookTable extends IdTable<Book, Isbn> {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(BookTable.class);
	@NotNull
	public static final BookTable INSTANCE = new BookTable();

	private BookTable() {
		super("book", "isbn");
	}

	@Override
	public boolean insert(@NotNull Book item) {
		var query = String.format("INSERT INTO %s(isbn, title, subtitle, author, publisher, format) VALUES(?, ?, ?, ?, ?, ?);", tableName);
		return insert(query, item.getISBN(), item.getTitle(), item.getSubtitle(), item.getAuthor(), item.getPublisher(), item.getFormat().ordinal());
	}

	@Override
	public boolean update(@NotNull Book item) {
		var query = String.format("UPDATE %s SET title = ?, subtitle = ?, author = ?, publisher = ?, format = ? WHERE isbn = ?;", tableName);
		return update(query, item.getTitle(), item.getSubtitle(), item.getAuthor(), item.getPublisher(), item.getFormat().ordinal(), item.getISBN());
	}

	@Override
	protected void createTable() {
		var query = String.format("CREATE TABLE %s(isbn TEXT PRIMARY KEY NOT NULL UNIQUE, title TEXT, subtitle TEXT, author TEXT NOT NULL, publisher TEXT NOT NULL, format INTEGER NOT NULL DEFAULT(0));", tableName);
		insert(query);
	}

	@Nullable
	@Override
	protected Book parse(@NotNull ResultSet result) throws SQLException {
		return new Book(Isbn.of(result.getString("isbn")), result.getString("title"), result.getString("subtitle"), result.getString("author"), result.getString("publisher"), Format.values()[result.getInt("format")]);
	}
}