package macro.library.book

import macro.library.database.IdTable
import java.sql.ResultSet
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-01
 */
data class Book(
	val uuid: UUID = UUID.randomUUID(),
	var isbn: Long? = null,
	var name: String,
	var author: String? = null,
	var series: String? = null,
	var seriesNum: Int? = null,
	var format: Format = Format.PAPERBACK
) : Comparable<Book> {
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
		return compareBy<Book> { it.series }
			.thenBy { it.seriesNum }
			.thenBy { it.isbn }
			.thenBy { it.format }
			.compare(this, other)
	}
}

object BookTable : IdTable<Book, UUID>(tableName = "book", idName = "uuid") {
	override fun insert(item: Book): Boolean {
		val query =
			"INSERT INTO $tableName(uuid, isbn, name, author, series, seriesNum, format) VALUES(?, ?, ?, ?, ?, ?, ?);"
		return insert(
			item.uuid,
			item.isbn,
			item.name,
			item.author,
			item.series,
			item.seriesNum,
			item.format.ordinal,
			query = query
		)
	}

	override fun update(item: Book): Boolean {
		val query =
			"UPDATE $tableName SET isbn = ?, name = ?, author = ?, series = ?, seriesNum = ?, format = ? WHERE uuid = ?;"
		return update(
			item.isbn,
			item.name,
			item.author,
			item.series,
			item.seriesNum,
			item.format,
			item.uuid,
			query = query
		)
	}

	override fun createTable() {
		val query =
			"CREATE TABLE $tableName(uuid TEXT PRIMARY KEY NOT NULL UNIQUE, isbn BIGINT UNIQUE, name TEXT NOT NULL, author TEXT, series TEXT, seriesNum INTEGER, format INTEGER);"
		insert(query = query)
	}

	override fun parse(result: ResultSet): Book {
		var isbn: Long? = result.getLong("isbn")
		if (isbn == 0L)
			isbn = null
		var seriesNum: Int? = result.getInt("seriesNum")
		if (seriesNum == 0)
			seriesNum = null
		return Book(
			uuid = UUID.fromString(result.getString("uuid")),
			isbn = isbn,
			name = result.getString("name"),
			author = result.getString("author"),
			series = result.getString("series"),
			seriesNum = seriesNum,
			format = Format.values()[result.getInt("format")]
		)
	}

	fun selectUnique(isbn: Long?): Book? {
		val query = "SELECT * FROM $tableName WHERE isbn LIKE ?;"
		return search(isbn, query = query).firstOrNull()
	}
}