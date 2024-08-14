package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.DescriptionSerializer
import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Work(
    val authors: List<Author> = emptyList(),
    val covers: List<Long> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime?,
    @Serializable(with = DescriptionSerializer::class)
    val description: String? = null,
    val key: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModified: LocalDateTime?,
    val latestRevision: Int,
    val revision: Int,
    val subjectPeople: List<String> = emptyList(),
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
}
