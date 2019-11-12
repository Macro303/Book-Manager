package macro.library.database

import macro.library.book.Isbn
import macro.library.book.WishlistBook
import org.apache.logging.log4j.LogManager
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Macro303 on 2019-Oct-31
 */
object WishlistTable : IdTable<WishlistBook, Isbn>(tableName = "wishlist", idName = "bookId") {
	private val LOGGER = LogManager.getLogger(WishlistTable::class.java)

	override fun insert(item: WishlistBook): Boolean {
		val query = "INSERT INTO $tableName(bookId, count) VALUES(?, ?);"
		return insert(item.bookId, item.count, query = query)
	}

	override fun update(item: WishlistBook): Boolean {
		val query = "UPDATE $tableName SET count = ? WHERE bookId = ?;"
		return update(item.count, item.bookId, query = query)
	}

	override fun createTable() {
		val query = "CREATE TABLE $tableName(" +
				"bookId TEXT PRIMARY KEY NOT NULL UNIQUE, " +
				"count INTEGER NOT NULL DEFAULT(1));"
		insert(query = query)
	}

	@Throws(SQLException::class)
	override fun parse(result: ResultSet): WishlistBook {
		return WishlistBook(
			Isbn.of(result.getString("bookId"))!!,
			result.getInt("count")
		)
	}

	fun search(title: String? = null, subtitle: String? = null, author: String? = null): List<WishlistBook> {
		val books = BookTable.search(title, subtitle, author).map { it.isbn }
		val query = "SELECT * FROM $tableName WHERE bookId IN (${books.joinToString(
			separator = "', '",
			prefix = "'",
			postfix = "'"
		)});"
		return search(query = query)
	}
}