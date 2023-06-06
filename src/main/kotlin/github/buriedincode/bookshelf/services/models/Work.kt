package github.buriedincode.bookshelf.services.models

data class Resource(
    val key: String
)

data class Author(
    val author: Resource
) {
    val authorId: String
        get() = author.key.split("/").last()
}

data class Work(
    val authors: List<Author> = ArrayList(),
    val description: String? = null,
)