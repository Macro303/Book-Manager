package github.buriedincode.bookshelf.docs

class GenreEntry(
    val genreId: Long,
    val title: String,
)

class Genre(
    val genreId: Long,
    val books: List<BookEntry>,
    val title: String,
)
