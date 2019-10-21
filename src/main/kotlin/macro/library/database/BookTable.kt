package macro.library.database

import macro.library.Isbn
import macro.library.book.Book
import macro.library.book.Format
import java.sql.ResultSet
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-22
 */
object BookTable : IdTable<Book, Isbn>(tableName = "book", idName = "isbn") {
	override fun insert(item: Book): Boolean {
		val query =
			"INSERT INTO $tableName(isbn, title, subtitle, author, publisher, format) VALUES(?, ?, ?, ?, ?, ?);"
		return insert(
			item.isbn,
			item.title,
			item.subtitle,
			item.author,
			item.publisher,
			item.format.ordinal,
			query = query
		)
	}

	override fun update(item: Book): Boolean {
		val query =
			"UPDATE $tableName SET title = ?, subtitle = ?, author = ?, publisher = ?, format = ? WHERE isbn = ?;"
		return update(
			item.title,
			item.subtitle,
			item.author,
			item.publisher,
			item.format.ordinal,
			item.isbn,
			query = query
		)
	}

	override fun createTable() {
		val query =
			"CREATE TABLE $tableName(isbn TEXT PRIMARY KEY NOT NULL UNIQUE, title TEXT, subtitle TEXT, author TEXT NOT NULL, publisher TEXT NOT NULL, format INTEGER NOT NULL DEFAULT(0));"
		insert(query = query)
	}

	override fun parse(result: ResultSet): Book {
		return Book(
			Isbn.of(result.getString("isbn")),
			result.getString("title"),
			result.getString("subtitle"),
			result.getString("author"),
			result.getString("publisher"),
			Format.values()[result.getInt("format")]
		)
	}

	fun selectUnique(isbn: Isbn?): Book? {
		val query = "SELECT * FROM $tableName WHERE isbn LIKE ?;"
		return search(isbn, query = query).firstOrNull()
	}
}