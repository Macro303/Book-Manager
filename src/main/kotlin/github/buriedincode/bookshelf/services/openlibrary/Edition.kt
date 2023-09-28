package github.buriedincode.bookshelf.services.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

@JsonIgnoreProperties(ignoreUnknown = true)
data class Contributor(
    val name: String,
    val role: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifiers(
    val goodreads: List<String> = ArrayList(),
    val google: List<String> = ArrayList(),
    val librarything: List<String> = ArrayList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Edition(
    val contributors: List<Contributor> = ArrayList(),
    @JsonDeserialize(using = DescriptionDeserializer::class)
    val description: String? = null,
    @JsonProperty("physical_format")
    val format: String? = null,
    val genres: List<String> = ArrayList(),
    val identifiers: Identifiers = Identifiers(),
    @JsonProperty("isbn_10")
    val isbn10: List<String> = ArrayList(),
    @JsonProperty("isbn_13")
    val isbn13: List<String> = ArrayList(),
    val key: String,
    @JsonProperty("publish_date")
    val publishDateStr: String? = null,
    val publishers: List<String> = ArrayList(),
    val subtitle: String? = null,
    val title: String,
    val works: List<Resource> = ArrayList(),
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
