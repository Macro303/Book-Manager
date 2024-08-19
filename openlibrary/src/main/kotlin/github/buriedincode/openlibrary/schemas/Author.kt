package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val alternateNames: List<String> = emptyList(),
    val bio: String? = null,
    val birthDate: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime? = null,
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
    data class Link(
        val title: String,
        val type: Resource,
        val url: String,
    )

    @Serializable
    data class RemoteIds(
        val isni: String,
        val viaf: String,
        val wikidata: String,
    )
}
