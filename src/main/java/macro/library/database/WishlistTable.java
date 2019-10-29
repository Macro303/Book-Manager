package macro.library.database;

import macro.library.book.Isbn;
import macro.library.book.WishlistBook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Macro303 on 2019-Oct-29
 */
public class WishlistTable extends IdTable<WishlistBook, Isbn> {
	@NotNull
	private static final Logger LOGGER = LogManager.getLogger(WishlistTable.class);
	@NotNull
	public static final WishlistTable INSTANCE = new WishlistTable();

	private WishlistTable() {
		super("wishlist", "bookId");
	}

	@Override
	public boolean insert(@NotNull WishlistBook item) {
		var query = String.format("INSERT INTO %s(bookId, count) VALUES(?, ?);", tableName);
		return insert(query, item.getBookId(), item.getCount());
	}

	@Override
	public boolean update(@NotNull WishlistBook item) {
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
	protected WishlistBook parse(@NotNull ResultSet result) throws SQLException {
		return new WishlistBook(
				Isbn.of(result.getString("bookId")),
				result.getInt("count")
		);
	}
}