package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val id: Long,
    val key: String,
    val lastModified: TypedResource,
    val name: String,
    val revision: Int,
    val type: Resource,
)
