package macro.library.database

import macro.library.book.Book
import macro.library.book.Format
import macro.library.book.Isbn
import org.apache.logging.log4j.LogManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-31
 */
object BookTable : IdTable<Book, Isbn>(tableName = "book", idName = "isbn") {
	private val LOGGER = LogManager.getLogger(BookTable::class.java)

	override fun insert(item: Book): Boolean {
		val query = "INSERT INTO $tableName(isbn, title, subtitle, publisher, format, image_small, image_medium, image_large) VALUES(?, ?, ?, ?, ?, ?, ?, ?);"
		return insert(item.isbn, item.title, item.subtitle, item.publisher, item.format.ordinal, item.imageSmall, item.imageMedium, item.imageLarge, query = query)
	}

	override fun update(item: Book): Boolean {
		val query = "UPDATE $tableName SET title = ?, subtitle = ?, publisher = ?, format = ?, image_small = ?, image_medium = ?, image_large = ? WHERE isbn = ?;"
		return update(item.title, item.subtitle, item.publisher, item.format.ordinal, item.imageSmall, item.imageMedium, item.imageLarge, item.isbn, query = query)
	}

	override fun createTable() {
		val query = "CREATE TABLE $tableName(" +
				"isbn TEXT PRIMARY KEY NOT NULL UNIQUE, " +
				"title TEXT NOT NULL, " +
				"subtitle TEXT, " +
				"publisher TEXT NOT NULL, " +
				"format INTEGER NOT NULL DEFAULT(0)," +
				"image_small TEXT," +
				"image_medium TEXT," +
				"image_large TEXT);"
		insert(query = query)
	}

	@Throws(SQLException::class)
	override fun parse(result: ResultSet): Book {
		return Book(
			Isbn.of(result.getString("isbn"))!!,
			result.getString("title"),
			result.getString("subtitle"),
			result.getString("publisher"),
			Format.values()[result.getInt("format")],
			result.getString("image_small"),
			result.getString("image_medium"),
			result.getString("image_large")
		)
	}

	fun search(title: String? = null, subtitle: String? = null, author: String? = null): List<Book> {
		var query = "SELECT * FROM $tableName"
		val params = ArrayList<String>()
		if (title != null) {
			query += " ${if (query.endsWith(tableName)) "WHERE" else "AND"} title LIKE ?"
			params.add(title)
		}
		if (subtitle != null) {
			query += " ${if (query.endsWith(tableName)) "WHERE" else "AND"} subtitle LIKE ?"
			params.add(subtitle)
		}
		if (params.isNotEmpty())
			query += " COLLATE NOCASE"
		return search(*params.toTypedArray(), query = "$query;")
	}
}