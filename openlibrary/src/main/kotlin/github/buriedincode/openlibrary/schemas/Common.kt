package github.buriedincode.openlibrary.schemas

import kotlinx.serialization.Serializable

@Serializable
data class TypedResource(
    val type: String,
    val value: String,
)

@Serializable
data class Resource(
    val key: String,
)
