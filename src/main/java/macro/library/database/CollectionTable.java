package macro.library.database;

import macro.library.book.Isbn;
import macro.library.book.CollectionBook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public class CollectionTable extends IdTable<CollectionBook, Isbn> {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(CollectionTable.class);
	@NotNull
	public static final CollectionTable INSTANCE = new CollectionTable();

	private CollectionTable() {
		super("collection", "bookId");
	}

	@Override
	public boolean insert(@NotNull CollectionBook item) {
		var query = String.format("INSERT INTO %s(bookId, count) VALUES(?, ?);", tableName);
		return insert(query, item.getBookId(), item.getCount());
	}

	@Override
	public boolean update(@NotNull CollectionBook item) {
		var query = String.format("UPDATE %s SET count = ? WHERE bookId = ?;", tableName);
		return update(query, item.getCount(), item.getBookId());
	}

	@Override
	protected void createTable() {
		var query = String.format("CREATE TABLE %s(bookId TEXT PRIMARY KEY NOT NULL UNIQUE, count INTEGER NOT NULL DEFAULT(1));", tableName);
		insert(query);
	}

	@Nullable
	@Override
	protected CollectionBook parse(@NotNull ResultSet result) throws SQLException {
		return new CollectionBook(
				Isbn.of(result.getString("bookId")),
				result.getInt("count")
		);
	}
}