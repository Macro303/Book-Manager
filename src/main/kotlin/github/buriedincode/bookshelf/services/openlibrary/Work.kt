package github.buriedincode.bookshelf.services.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonIgnoreProperties(ignoreUnknown = true)
data class Resource(
    val key: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AuthorResource(
    val author: Resource,
) {
    val authorId: String
        get() = author.key.split("/").last()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Work(
    val authors: List<AuthorResource> = ArrayList(),
    @JsonDeserialize(using = DescriptionDeserializer::class)
    val description: String? = null,
)
