package github.buriedincode.bookshelf.docs

import github.buriedincode.bookshelf.models.Format
import java.time.LocalDate

class BookEntry(
    val bookId: Long,
    val description: String?,
    val format: Format,
    val goodreadsId: String?,
    val googleBooksId: String?,
    val imageUrl: String?,
    val isbn: String?,
    val isCollected: Boolean,
    val libraryThingId: String?,
    val openLibraryId: String?,
    val publishDate: LocalDate?,
    val publisherId: Long?,
    val subtitle: String?,
    val title: String,
)

class Book(
    val bookId: Long,
    val credits: List<CreatorRole>,
    val description: String?,
    val format: Format,
    val genres: List<GenreEntry>,
    val goodreadsId: String?,
    val googleBooksId: String?,
    val imageUrl: String?,
    val isbn: String?,
    val isCollected: Boolean,
    val libraryThingId: String?,
    val openLibraryId: String?,
    val publishDate: LocalDate?,
    val publisher: PublisherEntry,
    val readers: List<UserEntry>,
    val series: List<BookSeries>,
    val subtitle: String?,
    val title: String,
    val wishers: List<UserEntry>,
)

class CreatorRole(
    val creatorId: Long,
    val roleId: Long,
)

class BookSeries(
    val seriesId: Long,
    val number: Int?,
)
