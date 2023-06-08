package github.buriedincode.bookshelf.services.openlibrary

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Author(
    val key: String,
    val name: String
) {
    val authorId: String
        get() = key.split("/").last()
}