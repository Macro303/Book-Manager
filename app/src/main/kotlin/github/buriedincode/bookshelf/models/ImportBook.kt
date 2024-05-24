package github.buriedincode.bookshelf.models

data class ImportBook(
    val goodreadsId: String? = null,
    val googleBooksId: String? = null,
    val isbn: String? = null,
    val isCollected: Boolean = false,
    val libraryThingId: String? = null,
    val openLibraryId: String? = null,
)
