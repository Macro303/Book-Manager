package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.serializers.LocalDateTimeSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val id: Long,
    val key: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModified: LocalDateTime?,
    val name: String,
    val revision: Int,
    val type: Resource,
)
