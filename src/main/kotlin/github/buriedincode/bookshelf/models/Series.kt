package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Series(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Series> {
    companion object : LongEntityClass<Series>(SeriesTable), Logging {
        val comparator = compareBy(Series::title)
    }

    val books by BookSeries referrersOn BookSeriesTable.seriesCol
    var title: String by SeriesTable.titleCol

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "id" to id.value,
            "title" to title,
        )
        if (showAll) {
            output["books"] = books.sortedWith(
                compareBy<BookSeries> { it.number ?: Int.MAX_VALUE }.thenBy { it.book },
            ).map {
                mapOf(
                    "book" to it.book.toJson(),
                    "number" to it.number,
                )
            }
        }
        return output.toSortedMap()
    }

    override fun compareTo(other: Series): Int = comparator.compare(this, other)
}
