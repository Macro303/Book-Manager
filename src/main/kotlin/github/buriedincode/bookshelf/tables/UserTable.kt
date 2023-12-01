package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import github.buriedincode.bookshelf.models.UserRole
import github.buriedincode.bookshelf.tables.BookTable.default
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils

object UserTable : LongIdTable(name = "users"), Logging {
    val imageCol: Column<String?> = text(name = "image").nullable()
    val roleCol: Column<UserRole> = enumerationByName(
        name = "role",
        length = 12,
        klass = UserRole::class,
    ).default(defaultValue = UserRole.GUEST)
    val usernameCol: Column<String> = text(name = "username").uniqueIndex()

    init {
        Utils.query {
            SchemaUtils.create(this)
        }
    }
}
