package github.macro.external

import github.macro.Utils
import github.macro.book.Book
import kong.unirest.json.JSONArray
import kong.unirest.json.JSONObject
import github.macro.book.Isbn
import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Nov-12
 */
object OpenLibrary {
	private val LOGGER = LogManager.getLogger(OpenLibrary::class.java)
	private val URL = "http://openlibrary.org/api/books"

	fun searchBook(isbn: Isbn): Book? {
		val url = "$URL?bibkeys=ISBN:$isbn&format=json&jscmd=data"
		val request = Utils.httpRequest(url) ?: return null
		val response = request.getObject()
		val bookObj = response.optJSONObject("ISBN:$isbn") ?: return null
		val title = bookObj.getString("title")
		var subtitle: String? = bookObj.optString("subtitle")
		if (subtitle.isNullOrBlank())
			subtitle = null
		var publishers: JSONArray? = bookObj.optJSONArray("publishers")
		if (publishers == null)
			publishers = JSONArray()
		val publisherStr = StringBuilder()
		for (publisher in publishers) {
			if (publisherStr.isNotEmpty())
				publisherStr.append(";")
			publisherStr.append((publisher as JSONObject).getString("name"))
		}
		val idObj = bookObj.getJSONObject("identifiers")
		val googleBooksId = idObj.optJSONArray("google")?.map { it?.toString() }?.firstOrNull()
		val goodreadsId = idObj.optJSONArray("goodreads")?.map { it?.toString() }?.firstOrNull()
		val openLibraryId = idObj.optJSONArray("openlibrary")?.map { it?.toString() }?.firstOrNull()
		return Book(
			isbn = isbn,
			title = title,
			subtitle = subtitle,
			publisher = publisherStr.toString(),
			googleBooksId = googleBooksId,
			goodreadsId = goodreadsId,
			openLibraryId = openLibraryId
		).add()
	}

	fun selectBook(book: Book): Book {
		return book
	}
}