package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Author(
    val key: String,
    val name: String,
    val photos: List<Int> = emptyList(),
) {
    val authorId: String
        get() = key.split("/").last()
}
