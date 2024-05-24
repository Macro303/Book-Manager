package github.buriedincode.bookshelf.services

import github.buriedincode.bookshelf.Utils
import github.buriedincode.openlibrary.SQLiteCache
import github.buriedincode.openlibrary.schemas.Author
import github.buriedincode.openlibrary.schemas.Edition
import github.buriedincode.openlibrary.schemas.SearchResponse
import github.buriedincode.openlibrary.schemas.Work
import kotlin.io.path.div
import github.buriedincode.openlibrary.OpenLibrary as Session

object OpenLibrary {
    private val session = Session(cache = SQLiteCache(path = (Utils.CACHE_ROOT / "openlibrary.sqlite"), expiry = 14))

    fun getEdition(id: String): Edition = TODO()

    fun getEditionByISBN(isbn: String): Edition = TODO()

    fun getWork(id: String): Work = TODO()

    fun getAuthor(id: String): Author = TODO()

    fun search(params: Map<String, String>): List<SearchResponse.Work> = TODO()
}
