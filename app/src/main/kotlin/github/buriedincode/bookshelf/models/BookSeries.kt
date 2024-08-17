package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookSeriesTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BookSeries(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BookSeries>(BookSeriesTable) {
        fun find(book: Book, series: Series): BookSeries? {
            return BookSeries
                .find { (BookSeriesTable.bookCol eq book) and (BookSeriesTable.seriesCol eq series) }
                .firstOrNull()
        }

        fun findOrCreate(book: Book, series: Series): BookSeries {
            return find(book, series) ?: BookSeries.new {
                this.book = book
                this.series = series
            }
        }
    }

    var book: Book by Book referencedOn BookSeriesTable.bookCol
    var series: Series by Series referencedOn BookSeriesTable.seriesCol
    var number: Int? by BookSeriesTable.numberCol
}
