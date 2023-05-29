package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Series(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Series>(SeriesTable), Logging

    val books by BookSeries referrersOn BookSeriesTable.seriesCol
    var title: String by SeriesTable.titleCol

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "seriesId" to id.value,
            "title" to title
        )
        if (showAll) {
            output["books"] = books.map {
                mapOf(
                    "bookId" to it.book.id.value,
                    "number" to it.number
                )
            }
        }
        return output.toSortedMap()
    }
}

class SeriesInput(
    val books: List<SeriesBookInput> = ArrayList(),
    val title: String,
)

class SeriesBookInput(
    val bookId: Long,
    val number: Int? = null
)
