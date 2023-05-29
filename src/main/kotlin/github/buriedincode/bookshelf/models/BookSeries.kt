package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.tables.BookSeriesTable
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class BookSeries(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<BookSeries>(BookSeriesTable), Logging

    var book: Book by Book referencedOn BookSeriesTable.bookCol
    var series: Series by Series referencedOn BookSeriesTable.seriesCol
    var number: Int? by BookSeriesTable.numberCol
}
