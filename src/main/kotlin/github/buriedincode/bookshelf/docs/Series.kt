package github.buriedincode.bookshelf.docs

class SeriesEntry(
    val seriesId: Long,
    val title: String
)

class Series(
    val seriesId: Long,
    val books: List<SeriesBook>,
    val title: String,
)

class SeriesBook(
    val bookId: Long,
    val number: Int?
)
