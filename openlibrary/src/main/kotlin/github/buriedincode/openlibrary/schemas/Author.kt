package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Author(
    @Serializable(with = LocalDateTimeSerializer::class)
    val created: LocalDateTime? = null,
    val id: Long? = null,
    val key: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModified: LocalDateTime?,
    val latestRevision: Int? = null,
    val name: String,
    val revision: Int,
    val type: Resource,
)
