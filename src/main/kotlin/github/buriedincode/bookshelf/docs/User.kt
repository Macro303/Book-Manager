package github.buriedincode.bookshelf.docs

import java.time.LocalDate

class UserEntry(
    val userId: Long,
    val imageUrl: String?,
    val role: Short,
    val username: String,
)

class User(
    val userId: Long,
    val imageUrl: String?,
    val readBooks: List<ReadBook>,
    val role: Short,
    val username: String,
    val wishedBooks: List<BookEntry>,
)

class ReadBook(
    val book: BookEntry,
    val readDate: LocalDate,
)
