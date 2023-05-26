package github.buriedincode.bookshelf.docs

class UserEntry(
    val userId: Long,
    val imageUrl: String?,
    val role: Short,
    val username: String
)

class User(
    val userId: Long,
    val imageUrl: String?,
    val readBooks: List<BookEntry>,
    val role: Short,
    val username: String,
    val wishedBooks: List<BookEntry>
)
