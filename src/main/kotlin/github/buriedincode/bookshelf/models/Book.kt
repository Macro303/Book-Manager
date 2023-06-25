package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.Utils.DATE_FORMATTER
import github.buriedincode.bookshelf.tables.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDate

class Book(id: EntityID<Long>) : LongEntity(id), Comparable<Book> {
    companion object : LongEntityClass<Book>(BookTable), Logging {
        val comparator = compareBy<Book> { it.series.firstOrNull()?.series }
            .thenBy { it.series.firstOrNull()?.number ?: Int.MAX_VALUE }
            .thenBy(Book::title)
            .thenBy(nullsFirst(), Book::subtitle)
    }

    val credits by BookCreatorRole referrersOn BookCreatorRoleTable.bookCol
    var description: String? by BookTable.descriptionCol
    var format: Format by BookTable.formatCol
    var genres by Genre via BookGenreTable
    var goodreadsId: String? by BookTable.goodreadsCol
    var googleBooksId: String? by BookTable.googleBooksCol
    var imageUrl: String? by BookTable.imageUrlCol
    var isCollected: Boolean by BookTable.isCollectedCol
    var isbn: String? by BookTable.isbnCol
    var libraryThingId: String? by BookTable.libraryThingCol
    var openLibraryId: String? by BookTable.openLibraryCol
    var publishDate: LocalDate? by BookTable.publishDateCol
    var publisher: Publisher? by Publisher optionalReferencedOn BookTable.publisherCol
    val readers by ReadBook referrersOn ReadBookTable.bookCol
    val series by BookSeries referrersOn BookSeriesTable.bookCol
    var subtitle: String? by BookTable.subtitleCol
    var title: String by BookTable.titleCol
    var wishers by User via WishedTable

    fun toJson(showAll: Boolean = false): Map<String, Any?> {
        val output = mutableMapOf<String, Any?>(
            "bookId" to id.value,
            "description" to description,
            "format" to format.name,
            "goodreadsId" to goodreadsId,
            "googleBooksId" to googleBooksId,
            "imageUrl" to imageUrl,
            "isbn" to isbn,
            "isCollected" to isCollected,
            "libraryThingId" to libraryThingId,
            "openLibraryId" to openLibraryId,
            "publishDate" to publishDate?.format(DATE_FORMATTER),
            "subtitle" to subtitle,
            "title" to title
        )
        if (showAll) {
            output["credits"] = credits.sortedWith(compareBy<BookCreatorRole> { it.creator }.thenBy { it.role }).map {
                mapOf(
                    "creatorId" to it.creator.id.value,
                    "roleId" to it.role.id.value,
                )
            }
            output["genres"] = genres.sorted().map { it.toJson() }
            output["publisher"] = publisher?.toJson()
            output["readers"] = readers.sortedWith(compareBy<ReadBook> { it.user }.thenBy{ it.date }).map {
                mapOf(
                    "date" to it.date.format(DATE_FORMATTER),
                    "userId" to it.user.id.value,
                )
            }
            output["series"] = series.sortedWith(compareBy<BookSeries> { it.series }.thenBy{ it.number ?: Int.MAX_VALUE }).map {
                mapOf(
                    "seriesId" to it.series.id.value,
                    "number" to it.number
                )
            }
            output["wishers"] = wishers.sorted().map { it.toJson() }
        } else
            output["publisherId"] = publisher?.id?.value
        return output.toSortedMap()
    }

    override fun compareTo(other: Book): Int = comparator.compare(this, other)
}

data class BookInput(
    val credits: List<BookCreditInput> = ArrayList(),
    val description: String? = null,
    val format: Format = Format.PAPERBACK,
    val genreIds: List<Long> = ArrayList(),
    val goodreadsId: String? = null,
    val googleBooksId: String? = null,
    val imageUrl: String? = null,
    val isCollected: Boolean = false,
    val isbn: String? = null,
    val libraryThingId: String? = null,
    val openLibraryId: String? = null,
    val publishDate: LocalDate? = null,
    val publisherId: Long? = null,
    val readers: List<BookReaderInput> = ArrayList(),
    val series: List<BookSeriesInput> = ArrayList(),
    val subtitle: String? = null,
    val title: String,
    val wisherIds: List<Long> = ArrayList()
)

data class BookCreditInput(
    val creatorId: Long,
    val roleId: Long,
)

data class BookReaderInput(
    val userId: Long,
    val readDate: LocalDate = LocalDate.now()
)

data class BookSeriesInput(
    val seriesId: Long,
    val number: Int? = null
)

data class BookImport(
    val goodreadsId: String? = null,
    val googleBooksId: String? = null,
    val isCollected: Boolean = false,
    val isbn: String? = null,
    val libraryThingId: String? = null,
    val openLibraryId: String? = null,
    val wisherIds: List<Long> = ArrayList()
)
