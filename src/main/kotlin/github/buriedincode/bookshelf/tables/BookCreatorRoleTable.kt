package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils

object BookCreatorRoleTable : LongIdTable(name = "books__creators__roles"), Logging {
    val bookCol: Column<EntityID<Long>> = reference(
        name = "book_id",
        foreign = BookTable,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
    val creatorCol: Column<EntityID<Long>> = reference(
        name = "creator_id",
        foreign = CreatorTable,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
    val roleCol: Column<EntityID<Long>> = reference(
        name = "role_id",
        foreign = RoleTable,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )

    init {
        Utils.query {
            uniqueIndex(bookCol, creatorCol, roleCol)
            SchemaUtils.create(this)
        }
    }
}