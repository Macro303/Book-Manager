package github.buriedincode.bookshelf.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate

data class BookInput(
    val credits: List<Credit> = ArrayList(),
    val format: Format = Format.PAPERBACK,
    val genreIds: List<Long> = ArrayList(),
    val goodreadsId: String? = null,
    val googleBooksId: String? = null,
    val image: String? = null,
    val isCollected: Boolean = false,
    val isbn: String? = null,
    val libraryThingId: String? = null,
    val openLibraryId: String? = null,
    val publishDate: LocalDate? = null,
    val publisherId: Long? = null,
    val readers: List<Reader> = ArrayList(),
    val series: List<Series> = ArrayList(),
    val subtitle: String? = null,
    val summary: String? = null,
    val title: String,
    val wisherIds: List<Long> = ArrayList(),
) {
    data class Credit(
        val creatorId: Long,
        val roleId: Long,
    )

    data class Reader(
        val userId: Long,
        @JsonDeserialize(using = LocalDateDeserializer::class)
        val readDate: LocalDate? = LocalDate.now(),
    )

    data class Series(
        val seriesId: Long,
        val number: Int? = null,
    )
}
