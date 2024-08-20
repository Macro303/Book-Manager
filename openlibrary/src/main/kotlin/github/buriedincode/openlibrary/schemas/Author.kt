package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.LocalDateSerializer
import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val alternateNames: List<String> = emptyList(),
    val bio: String? = null,
    val birthDate: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime? = null,
    @Serializable(with = LocalDateSerializer::class)
    val deathDate: LocalDate? = null,
    val id: Long? = null,
    val key: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModified: LocalDateTime?,
    val latestRevision: Int? = null,
    val links: List<Link> = emptyList(),
    val name: String,
    val personalName: String? = null,
    val photos: List<Int> = emptyList(),
    val remoteIds: RemoteIds? = null,
    val revision: Int,
    val sourceRecords: List<String> = emptyList(),
    val title: String? = null,
    val type: Resource,
) {
    @Serializable
    data class RemoteIds(
        val isni: String? = null,
        val viaf: String? = null,
        val wikidata: String,
    )
}
