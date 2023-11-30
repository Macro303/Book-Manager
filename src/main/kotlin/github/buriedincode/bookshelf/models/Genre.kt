package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookGenreTable
import github.buriedincode.bookshelf.tables.GenreTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Genre(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Genre> {
    companion object : LongEntityClass<Genre>(GenreTable), Logging {
        val comparator = compareBy(Genre::title)
    }

    var books by Book via BookGenreTable
    var title: String by GenreTable.titleCol

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "id" to id.value,
            "title" to title,
        )
        if (showAll) {
            output["books"] = books.sorted().map { it.toJson() }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: Genre): Int = comparator.compare(this, other)
}
