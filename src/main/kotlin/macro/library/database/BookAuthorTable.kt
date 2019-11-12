package macro.library.database

import macro.library.book.author.Author
import macro.library.book.Book
import macro.library.book.Isbn
import org.apache.logging.log4j.LogManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-31
 */
object BookAuthorTable : Table<String>(tableName = "bookAuthor") {
	private val LOGGER = LogManager.getLogger(BookAuthorTable::class.java)

	override fun createTable() {
		val query = "CREATE TABLE $tableName(" +
				"bookId TEXT NOT NULL, " +
				"authorId TEXT NOT NULL, " +
				"PRIMARY KEY(bookId, authorId), " +
				"UNIQUE(bookId, authorId));"
		insert(query = query)
	}

	@Throws(SQLException::class)
	override fun parse(result: ResultSet): String = result.getString(1)

	fun searchAuthor(authorId: UUID): List<Book> {
		val query = "SELECT bookId FROM $tableName WHERE authorId = ?;"
		return search(authorId, query = query).mapNotNull { BookTable.selectUnique(unique = Isbn.of(it)!!) }
	}

	fun searchBook(bookId: Isbn): List<Author> {
		val query = "SELECT authorId FROM $tableName WHERE bookId = ?;"
		return search(bookId, query = query).mapNotNull { AuthorTable.selectUnique(unique = UUID.fromString(it)) }
	}

	fun insert(bookId: Isbn, authorId: UUID): Boolean {
		val query = "INSERT INTO $tableName(bookId, authorId) VALUES(?, ?);"
		return insert(bookId, authorId, query = query)
	}
}