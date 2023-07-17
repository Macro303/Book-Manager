package github.buriedincode.bookshelf.tables

import github.buriedincode.bookshelf.Utils
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table

object BookGenreTable : Table(name = "books__genres"), Logging {
    val bookCol: Column<EntityID<Long>> = reference(
        name = "book_id",
        foreign = BookTable,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
    val genreCol: Column<EntityID<Long>> = reference(
        name = "genre_id",
        foreign = GenreTable,
        onUpdate = ReferenceOption.CASCADE,
        onDelete = ReferenceOption.CASCADE
    )
    override val primaryKey = PrimaryKey(bookCol, genreCol)

    init {
        Utils.query {
            SchemaUtils.create(this)
        }
    }
}