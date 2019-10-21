package macro.library.book

import macro.library.Isbn
import macro.library.database.IdTable
import java.sql.ResultSet
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-01
 */
data class Book @JvmOverloads constructor(
	val isbn: Isbn,
	var title: String?,
	var subtitle: String?,
	var author: String,
	var publisher: String,
	var format: Format = Format.PAPERBACK,
	val uuid: UUID = UUID.randomUUID()
) : Comparable<Book> {

	init {
		if (title.isNullOrBlank())
			title = null
		if (subtitle.isNullOrBlank())
			subtitle = null
	}

	fun add(): Book {
		BookTable.insert(item = this)
		return this
	}

	fun push(): Book {
		BookTable.update(item = this)
		return this
	}

	fun remove() {
		BookTable.delete(id = this.uuid)
	}

	override fun compareTo(other: Book): Int {
		return compareBy<Book> { it.title }
			.thenBy { it.subtitle }
			.thenBy { it.format }
			.thenBy { it.isbn }
			.compare(this, other)
	}
}

object BookTable : IdTable<Book, UUID>(tableName = "book", idName = "uuid") {
	override fun insert(item: Book): Boolean {
		val query =
			"INSERT INTO $tableName(uuid, isbn, title, subtitle, author, publisher, format) VALUES(?, ?, ?, ?, ?, ?, ?);"
		return insert(
			item.uuid,
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
			"UPDATE $tableName SET isbn = ?, title = ?, subtitle = ?, author = ?, publisher = ?, format = ? WHERE uuid = ?;"
		return update(
			item.isbn,
			item.title,
			item.subtitle,
			item.author,
			item.publisher,
			item.format.ordinal,
			item.uuid,
			query = query
		)
	}

	override fun createTable() {
		val query =
			"CREATE TABLE $tableName(uuid TEXT PRIMARY KEY NOT NULL UNIQUE, isbn TEXT UNIQUE, title TEXT, subtitle TEXT, author TEXT NOT NULL, publisher TEXT NOT NULL, format INTEGER NOT NULL DEFAULT(0));"
		insert(query = query)
	}

	override fun parse(result: ResultSet): Book {
		return Book(
			uuid = UUID.fromString(result.getString("uuid")),
			isbn = Isbn.of(result.getString("isbn")),
			title = result.getString("title"),
			subtitle = result.getString("subtitle"),
			author = result.getString("author"),
			publisher = result.getString("publisher"),
			format = Format.values()[result.getInt("format")]
		)
	}

	fun selectUnique(isbn: Isbn?): Book? {
		val query = "SELECT * FROM $tableName WHERE isbn LIKE ?;"
		return search(isbn, query = query).firstOrNull()
	}
}