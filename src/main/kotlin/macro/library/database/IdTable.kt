package macro.library.database

import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-01
 */
abstract class IdTable<T, S>(tableName: String, internal val idName: String) : Table<T>(tableName = tableName) {
	private val LOGGER = LogManager.getLogger(IdTable::class.java)

	fun selectUnique(id: S): T? {
		val query = "SELECT * FROM $tableName WHERE $idName = ?;"
		return search(id, query = query).firstOrNull()
	}

	fun delete(id: S): Boolean {
		val query = "DELETE FROM $tableName WHERE $idName = ?;"
		return delete(id, query = query)
	}

	abstract fun insert(item: T): Boolean
	abstract fun update(item: T): Boolean
}