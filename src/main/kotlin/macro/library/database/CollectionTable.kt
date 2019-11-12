package macro.library.database

import macro.library.book.CollectionBook
import macro.library.book.Isbn
import org.apache.logging.log4j.LogManager
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Macro303 on 2019-Oct-30
 */
object CollectionTable : IdTable<CollectionBook, Isbn>(tableName = "collection", idName = "bookId") {
	private val LOGGER = LogManager.getLogger(CollectionTable::class.java)

	override fun insert(item: CollectionBook): Boolean {
		val query = "INSERT INTO $tableName(bookId, count) VALUES(?, ?);"
		return insert(item.bookId, item.count, query = query)
	}

	override fun update(item: CollectionBook): Boolean {
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
	override fun parse(result: ResultSet): CollectionBook {
		return CollectionBook(
			Isbn.of(result.getString("bookId"))!!,
			result.getInt("count")
		)
	}

	fun search(title: String? = null, subtitle: String? = null, author: String? = null): List<CollectionBook> {
		val books = BookTable.search(title, subtitle, author).map { it.isbn }
		val query = "SELECT * FROM $tableName WHERE bookId IN (${books.joinToString(
			separator = "', '",
			prefix = "'",
			postfix = "'"
		)});"
		return search(query = query)
	}
}