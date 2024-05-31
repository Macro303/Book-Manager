package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Edition (
    val authors: List<Resource> = emptyList(),
    val byStatement: String,
    val classifications: Classification,
    val contributions: List<String> = emptyList(),
    val contributors: List<Contributor> = emptyList(),
    val covers: List<Int> = emptyList(),
    val created: TypedResource,
    val deweyDecimalClass: List<String> = emptyList(),
    val identifiers: Identifiers,
    @JsonNames("isbn_10")
    val isbn10: List<String> = emptyList(),
    @JsonNames("isbn_13")
    val isbn13: List<String> = emptyList(),
    val key: String,
    val languages: List<Resource> = emptyList(),
    val lastModified: TypedResource,
    val latestRevision: Int,
    val lcClassifications: List<String> = emptyList(),
    val lccn: List<String> = emptyList(),
    val localId: List<String> = emptyList(),
    val notes: String,
    val numberOfPages: Int,
    val oclcNumbers: List<String> = emptyList(),
    val otherTitles: List<String> = emptyList(),
    val pagination: String,
    val physicalFormat: String,
    val publishCountry: String,
    val publishDate: String? = null,
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
