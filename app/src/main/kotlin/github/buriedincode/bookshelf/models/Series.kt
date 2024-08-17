package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookSeriesTable
import github.buriedincode.bookshelf.tables.SeriesTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Series(id: EntityID<Long>) : LongEntity(id), IJson, Comparable<Series> {
    companion object : LongEntityClass<Series>(SeriesTable) {
        val comparator = compareBy(Series::title)

        fun find(title: String): Series? {
            return Series.find { SeriesTable.titleCol eq title }.firstOrNull()
        }

        fun findOrCreate(title: String): Series {
            return find(title) ?: Series.new {
                this.title = title
            }
        }
    }

    val books by BookSeries referrersOn BookSeriesTable.seriesCol
    var summary: String? by SeriesTable.summaryCol
    var title: String by SeriesTable.titleCol

    val firstBook: BookSeries?
        get() = books.sortedWith(compareBy<BookSeries> { it.number ?: Int.MAX_VALUE }.thenBy { it.book }).firstOrNull()

    override fun toJson(showAll: Boolean): Map<String, Any?> {
        return mutableMapOf<String, Any?>(
            "id" to id.value,
            "imageUrl" to firstBook?.book?.imageUrl,
            "summary" to summary,
            "title" to title,
        ).apply {
            if (showAll) {
                put("books", books.sortedBy { it.book }.map { it.book.id.value to it.number })
            }
        }.toSortedMap()
    }

    override fun compareTo(other: Series): Int = comparator.compare(this, other)
}

data class SeriesInput(
    val books: List<Book> = emptyList(),
    val summary: String? = null,
    val title: String,
) {
    data class Book(
        val book: Long,
        val number: Int? = null,
    )
}
