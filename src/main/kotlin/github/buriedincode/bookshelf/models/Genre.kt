package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookGenreTable
import github.buriedincode.bookshelf.tables.GenreTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Genre(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Genre>(GenreTable), Logging

    var books by Book via BookGenreTable
    var title: String by GenreTable.titleCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "genreId" to id.value,
            "title" to title
        )
        if (showAll)
            output["books"] = books.map { it.toJson() }
        return output.toSortedMap()
    }
}

class GenreInput(
    val bookIds: List<Long> = ArrayList(),
    val title: String
)
