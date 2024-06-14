package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.LocalDateSerializer
import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Edition(
    val authors: List<Resource> = emptyList(),
    val byStatement: String? = null,
    val classifications: Classification,
    val contributions: List<String> = emptyList(),
    val contributors: List<Contributor> = emptyList(),
    val covers: List<Int> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime?,
    val deweyDecimalClass: List<String> = emptyList(),
    val fullTitle: String? = null,
    val identifiers: Identifiers,
    @JsonNames("isbn_10")
    val isbn10: List<String> = emptyList(),
    @JsonNames("isbn_13")
    val isbn13: List<String> = emptyList(),
    val key: String,
    val languages: List<Resource> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModified: LocalDateTime?,
    val latestRevision: Int,
    val lcClassifications: List<String> = emptyList(),
    val lccn: List<String> = emptyList(),
    val localId: List<String> = emptyList(),
    val notes: String? = null,
    val numberOfPages: Int,
    val oclcNumbers: List<String> = emptyList(),
    val otherTitles: List<String> = emptyList(),
    val pagination: String? = null,
    val physicalFormat: String,
    val publishCountry: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val publishDate: LocalDate? = null,
    val publishers: List<String> = emptyList(),
    val revision: Int,
    val sourceRecords: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val subtitle: String? = null,
    val translatedFrom: List<Resource> = emptyList(),
    val title: String,
    val type: Resource,
    val works: List<Resource> = emptyList(),
) {
    @Serializable
    data class Classification(
        val key: String? = null,
    )

    @Serializable
    data class Contributor(
        val name: String,
        val role: String,
    )

    @Serializable
    data class Identifiers(
        val goodreads: List<String> = emptyList(),
        val google: List<String> = emptyList(),
        val librarything: List<String> = emptyList(),
    )
}
