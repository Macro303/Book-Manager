package github.buriedincode.bookshelf.docs

class RoleEntry(
    val roleId: Long,
    val title: String
)

class Role(
    val roleId: Long,
    val bookCreators: List<BookCreator>,
    val title: String,
)

class BookCreator(
    val book: BookEntry,
    val creator: CreatorEntry
)
