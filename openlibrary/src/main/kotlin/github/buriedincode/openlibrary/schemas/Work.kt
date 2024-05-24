package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.Serializable

@Serializable
data class Resource(
    val key: String,
)

@Serializable
data class AuthorResource(
    val author: Resource,
) {
    val authorId: String
        get() = author.key.split("/").last()
}

@Serializable
data class Work(
    val authors: List<AuthorResource> = emptyList(),
    val description: String? = null,
)
