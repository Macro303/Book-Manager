package github.buriedincode.bookshelf.models

data class SeriesInput(
    val books: List<Book> = ArrayList(),
    val title: String,
) {
    data class Book(
        val bookId: Long,
        val number: Int? = null,
    )
}
