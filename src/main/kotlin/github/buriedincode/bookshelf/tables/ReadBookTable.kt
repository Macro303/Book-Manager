package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

object ReadBookTable : LongIdTable(name = "read_books"), Logging {
    val bookCol: Column<EntityID<Long>> = reference(
        name = "book_id",
        foreign = BookTable,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
    val userCol: Column<EntityID<Long>> = reference(
        name = "user_id",
        foreign = UserTable,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
    val readDateCol: Column<LocalDate?> = date(name = "read_date").nullable()

    init {
        Utils.query(description = "Create Read Book Table") {
            uniqueIndex(bookCol, userCol)
            SchemaUtils.create(this)
        }
    }
}