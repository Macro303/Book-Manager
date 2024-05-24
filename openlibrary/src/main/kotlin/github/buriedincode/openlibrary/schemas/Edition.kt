package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Contributor(
    val name: String,
    val role: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Identifiers(
    val goodreads: List<String> = emptyList(),
    val google: List<String> = emptyList(),
    val librarything: List<String> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Edition(
    val contributors: List<Contributor> = emptyList(),
    val description: String? = null,
    @JsonNames("physical_format")
    val format: String? = null,
    val genres: List<String> = emptyList(),
    val identifiers: Identifiers = Identifiers(),
    @JsonNames("isbn_10")
    val isbn10: List<String> = emptyList(),
    @JsonNames("isbn_13")
    val isbn13: List<String> = emptyList(),
    val key: String,
    @JsonNames("publish_date")
    val publishDateStr: String? = null,
    val publishers: List<String> = emptyList(),
    val subtitle: String? = null,
    val title: String,
    val works: List<Resource> = emptyList(),
) {
    val editionId: String
        get() = key.split("/").last()
    val isbn: String?
        get() = isbn13.firstOrNull() ?: isbn10.firstOrNull()
    val publishDate: LocalDate?
        get() {
            if (publishDateStr == null) {
                return null
            }
            try {
                LocalDate.parse(publishDateStr, DateTimeFormatter.ISO_DATE)
            } catch (err: DateTimeParseException) {
                for (pattern in arrayOf(
                    "MMMM d, yyyy",
                    "yyyy-MMM-dd",
                    "MMM dd, yyyy",
                    "yyyy",
                    "MMMM yyyy",
                    "MMM, yyyy",
                    "d MMMM yyyy",
                    "dd/MM/yyyy",
                )) {
                    try {
                        return LocalDate.parse(
                            publishDateStr,
                            DateTimeFormatterBuilder()
                                .appendPattern(pattern)
                                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                                .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                                .toFormatter(),
                        )
                    } catch (_: DateTimeParseException) {
                    }
                }
                throw err
            }
            return null
        }
}
