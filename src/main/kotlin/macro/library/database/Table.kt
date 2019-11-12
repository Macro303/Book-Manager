package macro.library.database

import macro.library.Util
import org.apache.logging.log4j.LogManager
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-30
 */
abstract class Table<T> protected constructor(protected val tableName: String) {

	init {
		if (!this.exists())
			this.createTable()
	}

	private fun exists(): Boolean {
		val query = "SELECT name FROM sqlite_master WHERE type = ? AND name = ?;"
		try {
			DriverManager.getConnection(Util.DATABASE_URL).use { conn ->
				conn.prepareStatement(query).use { statement ->
					statement.setString(1, "table")
					statement.setString(2, tableName)
					val results = statement.executeQuery()
					return results.next()
				}
			}
		} catch (sqle: SQLException) {
			return false
		}

	}

	protected abstract fun createTable()

	fun dropTable() {
		val query = "DROP TABLE $tableName"
		delete(query = query)
	}

	protected fun delete(vararg values: Any?, query: String): Boolean = update(values = *values, query = query)
	protected fun insert(vararg values: Any?, query: String): Boolean = update(values = *values, query = query)

	protected fun update(vararg values: Any?, query: String): Boolean {
		LOGGER.debug("$query, ${values.contentToString()}")
		try {
			DriverManager.getConnection(Util.DATABASE_URL).use { conn ->
				conn.autoCommit = false
				try {
					conn.prepareStatement(query).use { statement ->
						values.forEachIndexed { index, value ->
							statement.setObject(index + 1, value)
						}
						statement.executeUpdate()
						conn.commit()
						return true
					}
				} catch (sqle: SQLException) {
					conn.rollback()
					throw sqle
				}
			}
		} catch (sqle: SQLException) {
			LOGGER.error("Unable to Execute: $query, ${values.contentToString()} => ${sqle.localizedMessage}")
		}
		return false
	}

	fun searchAll(): List<T> {
		val query = "SELECT * FROM $tableName"
		return search(query = query)
	}

	protected fun search(vararg values: Any?, query: String): List<T> {
		val items = ArrayList<T>()
		try {
			DriverManager.getConnection(Util.DATABASE_URL).use { conn ->
				conn.prepareStatement(query).use { statement ->
					values.forEachIndexed { index, value ->
						statement.setObject(index + 1, value)
					}
					val results = statement.executeQuery()
					while (results.next())
						items.add(parse(results))
				}
			}
		} catch (sqle: SQLException) {
			LOGGER.error("Unable to Execute: $query, ${values.contentToString()} => ${sqle.localizedMessage}")
			items.clear()
		}
		return items
	}

	@Throws(SQLException::class)
	protected abstract fun parse(result: ResultSet): T

	companion object {
		private val LOGGER = LogManager.getLogger(Table::class.java)
	}
}