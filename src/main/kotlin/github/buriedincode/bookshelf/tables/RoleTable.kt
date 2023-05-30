package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object RoleTable : LongIdTable(name = "roles"), Logging {
    val titleCol: Column<String> = text(name = "title").uniqueIndex()

    init {
        Utils.query(description = "Create Role Table") {
            SchemaUtils.create(this)
        }
    }
}