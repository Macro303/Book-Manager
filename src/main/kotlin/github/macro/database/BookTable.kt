package github.macro.database

import github.macro.book.Book
import github.macro.book.Format
import github.macro.book.Isbn
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
		val query =
			"INSERT INTO $tableName(isbn, title, subtitle, publisher, format, open_library_id, google_books_id, goodreads_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?);"
		return insert(
			item.isbn,
			item.title,
			item.subtitle,
			item.publisher,
			item.format.ordinal,
			item.openLibraryId,
			item.googleBooksId,
			item.goodreadsId,
			query = query
		)
	}

	override fun update(item: Book): Boolean {
		val query =
			"UPDATE $tableName SET title = ?, subtitle = ?, publisher = ?, format = ?, open_library_id = ?, google_books_id = ?, goodreads_id = ? WHERE isbn = ?;"
		return update(
			item.title,
			item.subtitle,
			item.publisher,
			item.format.ordinal,
			item.openLibraryId,
			item.googleBooksId,
			item.goodreadsId,
			item.isbn,
			query = query
		)
	}

	override fun createTable() {
		val query = "CREATE TABLE $tableName(" +
				"isbn TEXT PRIMARY KEY NOT NULL UNIQUE, " +
				"title TEXT NOT NULL, " +
				"subtitle TEXT, " +
				"publisher TEXT NOT NULL, " +
				"format INTEGER NOT NULL DEFAULT(0)," +
				"open_library_id TEXT," +
				"google_books_id TEXT," +
				"goodreads_id TEXT);"
		insert(query = query)
	}

	@Throws(SQLException::class)
	override fun parse(result: ResultSet): Book {
		return Book(
			isbn = Isbn.of(result.getString("isbn"))!!,
			title = result.getString("title"),
			subtitle = result.getString("subtitle"),
			publisher = result.getString("publisher"),
			format = Format.values()[result.getInt("format")],
			openLibraryId = result.getString("open_library_id"),
			googleBooksId = result.getString("google_books_id"),
			goodreadsId = result.getString("goodreads_id")
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