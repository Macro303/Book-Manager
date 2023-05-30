package github.buriedincode.bookshelf.models

import github.buriedincode.bookshelf.Utils.DATE_FORMATTER
import github.buriedincode.bookshelf.tables.*
import org.apache.logging.log4j.kotlin.Logging
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDate

class Book(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Book>(BookTable), Logging

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
    var readers by User via ReadTable
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
            "librayThingId" to libraryThingId,
            "openLibraryId" to openLibraryId,
            "publishDate" to publishDate?.format(DATE_FORMATTER),
            "subtitle" to subtitle,
            "title" to title
        )
        if (showAll) {
            output["credits"] = credits.map {
                mapOf(
                    "creatorId" to it.creator.id.value,
                    "roleId" to it.role.id.value,
                )
            }
            output["genres"] = genres.map { it.toJson() }
            output["publisher"] = publisher?.toJson()
            output["readers"] = readers.map { it.toJson() }
            output["series"] = series.map {
                mapOf(
                    "seriesId" to it.series.id.value,
                    "number" to it.number
                )
            }
            output["wishers"] = wishers.map { it.toJson() }
        } else
            output["publisherId"] = publisher?.id?.value
        return output.toSortedMap()
    }
}

class BookInput(
    val credits: List<CreatorRoleInput> = ArrayList(),
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
    val readerIds: List<Long> = ArrayList(),
    val series: List<BookSeriesInput> = ArrayList(),
    val subtitle: String? = null,
    val title: String,
    val wisherIds: List<Long> = ArrayList()
)

class CreatorRoleInput(
    val creatorId: Long,
    val roleId: Long,
)

class BookSeriesInput(
    val seriesId: Long,
    val number: Int? = null
)

class BookImport(
    val goodreadsId: String? = null,
    val googleBooksId: String? = null,
    val isCollected: Boolean = false,
    val isbn: String? = null,
    val libraryThingId: String? = null,
    val openLibraryId: String? = null,
    val wisherIds: List<Long> = ArrayList()
)
