package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.DescriptionSerializer
import github.buriedincode.openlibrary.serializers.LocalDateSerializer
import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Work(
    val authors: List<Author> = emptyList(),
    val coverEdition: Resource? = null,
    val covers: List<Long> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime?,
    @Serializable(with = DescriptionSerializer::class)
    val description: String? = null,
    val excerpts: List<Excerpt> = emptyList(),
    @Serializable(with = LocalDateSerializer::class)
    val firstPublishDate: LocalDate? = null,
    val firstSentence: TypedResource? = null,
    val key: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModified: LocalDateTime?,
    val latestRevision: Int,
    val links: List<Link> = emptyList(),
    val location: String? = null,
    val revision: Int,
    val subjectPeople: List<String> = emptyList(),
    val subjectPlaces: List<String> = emptyList(),
    val subjectTimes: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val subtitle: String? = null,
    val title: String,
    val type: Resource,
) {
    @Serializable
    data class Author(
        val author: Resource,
        val type: Resource,
    )

    @Serializable
    data class Excerpt(
        val excerpt: String,
    )
}
