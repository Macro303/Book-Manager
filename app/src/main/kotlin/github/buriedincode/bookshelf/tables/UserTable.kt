package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object UserTable : LongIdTable(name = "users") {
    val imageUrlCol: Column<String?> = text(name = "image_url").nullable()
    val usernameCol: Column<String> = text(name = "username").uniqueIndex()

    init {
        Utils.queryTransaction {
            SchemaUtils.create(this)
        }
    }
}
