package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.Serializable

@Serializable
data class Work(
    val authors: List<Author> = emptyList(),
    val created: TypedResource,
    val description: TypedResource,
    val key: String,
    val lastModified: TypedResource,
    val latestRevision: Int,
    val revision: Int,
    val title: String,
    val type: Resource,
) {
    @Serializable
    data class Author(
        val author: Resource,
        val type: Resource,
    )
}
