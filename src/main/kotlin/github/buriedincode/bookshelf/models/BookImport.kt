package github.buriedincode.bookshelf.models

data class BookImport(
    val goodreadsId: String? = null,
    val googleBooksId: String? = null,
    val isCollected: Boolean = false,
    val isbn: String? = null,
    val libraryThingId: String? = null,
    val openLibraryId: String? = null,
    val wisherIds: List<Long> = ArrayList(),
)
