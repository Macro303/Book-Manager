package macro.library.database

import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-30
 */
abstract class IdTable<T, S> protected constructor(tableName: String, protected val idName: String) :
	Table<T>(tableName = tableName) {

	fun selectUnique(unique: S): T? {
		val query = "SELECT * FROM $tableName WHERE $idName = ?"
		return search(unique, query = query).firstOrNull()
	}

	fun delete(unique: S): Boolean {
		val query = "DELETE FROM $tableName WHERE $idName = ?;"
		return delete(unique, query = query)
	}

	abstract fun insert(item: T): Boolean

	abstract fun update(item: T): Boolean

	companion object {
		private val LOGGER = LogManager.getLogger(IdTable::class.java)
	}
}