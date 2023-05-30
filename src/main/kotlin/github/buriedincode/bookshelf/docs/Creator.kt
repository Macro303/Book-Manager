package github.buriedincode.bookshelf.docs

class CreatorEntry(
    val creatorId: Long,
    val name: String,
    val imageUrl: String?
)

class Creator(
    val creatorId: Long,
    val bookRoles: List<BookRole>,
    val name: String,
    val imageUrl: String?
)

class BookRole(
    val book: BookEntry,
    val role: RoleEntry
)
