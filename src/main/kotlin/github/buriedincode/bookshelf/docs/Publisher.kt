package github.buriedincode.bookshelf.docs

class PublisherEntry(
    val publisherId: Long,
    val title: String
)

class Publisher(
    val publisherId: Long,
    val books: List<BookEntry>,
    val title: String,
)
