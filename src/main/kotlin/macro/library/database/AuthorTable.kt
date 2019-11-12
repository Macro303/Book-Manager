package macro.library.database

import macro.library.book.author.Author
import org.apache.logging.log4j.LogManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-31
 */
object AuthorTable : IdTable<Author, UUID>(tableName = "author", idName = "uuid") {
	private val LOGGER = LogManager.getLogger(AuthorTable::class.java)

	override fun insert(item: Author): Boolean {
		val query = "INSERT INTO $tableName(uuid, firstName, lastName, middleNames) VALUES(?, ?, ?, ?);"
		return insert(item.uuid, item.firstName, item.lastName, item.middleNames?.joinToString(";"), query = query)
	}

	override fun update(item: Author): Boolean {
		val query = "UPDATE $tableName SET firstName = ?, lastName = ?, middleNames = ? WHERE uuid = ?;"
		return update(item.firstName, item.lastName, item.middleNames?.joinToString(";"), item.uuid, query = query)
	}

	override fun createTable() {
		val query = "CREATE TABLE $tableName(" +
				"uuid TEXT PRIMARY KEY NOT NULL UNIQUE, " +
				"firstName TEXT NOT NULL, " +
				"lastName TEXT NOT NULL, " +
				"middleNames TEXT, " +
				"UNIQUE(firstName, lastName));"
		insert(query = query)
	}

	@Throws(SQLException::class)
	override fun parse(result: ResultSet): Author {
		var middleNames = emptyList<String>()
		val middleStr = result.getString("middleNames")
		if (middleStr != null)
			middleNames = middleStr.split(";")
		return Author(
			UUID.fromString(result.getString("uuid")),
			result.getString("firstName"),
			result.getString("lastName"),
			middleNames
		)
	}

	fun select(firstName: String, lastName: String): Author? {
		val query = "SELECT * FROM $tableName WHERE firstName = ? AND lastName = ?;"
		return search(firstName, lastName, query = query).firstOrNull()
	}

	fun search(firstName: String? = null, lastName: String? = null): List<Author> {
		var query = "SELECT * FROM $tableName"
		val params = ArrayList<String>()
		if (firstName != null) {
			query += " ${if (query.endsWith(tableName)) "WHERE" else "AND"} firstName LIKE ?"
			params.add(firstName)
		}
		if (lastName != null) {
			query += " ${if (query.endsWith(tableName)) "WHERE" else "AND"} lastName LIKE ?"
			params.add(lastName)
		}
		if (params.isNotEmpty())
			query += " COLLATE NOCASE"
		return search(*params.toTypedArray(), query = query)
	}
}