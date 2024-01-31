package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object UserTable : LongIdTable(name = "users"), Logging {
    val imageUrlCol: Column<String?> = text(name = "image").nullable()
    val usernameCol: Column<String> = text(name = "username").uniqueIndex()

    init {
        Utils.query {
            SchemaUtils.create(this)
        }
    }
}
