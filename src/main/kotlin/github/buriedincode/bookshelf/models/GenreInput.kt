package github.buriedincode.bookshelf.models

data class GenreInput(
    val bookIds: List<Long> = ArrayList(),
    val summary: String? = null,
    val title: String,
)
