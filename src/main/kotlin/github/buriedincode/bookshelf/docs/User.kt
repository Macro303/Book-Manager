package github.buriedincode.bookshelf.docs

class UserEntry(
    val userId: Long,
    val username: String
)

class User(
    val userId: Long,
    val readBooks: List<BookEntry>,
    val username: String,
    val wishedBooks: List<BookEntry>
)
