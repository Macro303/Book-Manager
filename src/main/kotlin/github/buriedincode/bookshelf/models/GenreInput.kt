package github.buriedincode.bookshelf.models

data class GenreInput(
    val bookIds: List<Long> = ArrayList(),
    val title: String,
)
