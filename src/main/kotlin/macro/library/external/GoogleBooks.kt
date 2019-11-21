package macro.library.external

import kong.unirest.json.JSONObject
import macro.library.Util
import macro.library.book.Book
import macro.library.book.Isbn
import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Nov-13
 */
object GoogleBooks {
	private val LOGGER = LogManager.getLogger(GoogleBooks::class.java)
	private val URL = "https://www.googleapis.com/books/v1/volumes"

	fun selectBook(book: Book): Book {
		var request: JSONObject? = null
		if (book.googleBooksId != null) {
			val url = "$URL/${book.googleBooksId}"
			request = Util.httpRequest(url)?.`object`
		}
		if (request == null) {
			val url = "$URL?q=isbn:${book.isbn}"
			request = Util.httpRequest(url)?.`object`?.optJSONArray("items")?.optJSONObject(0) ?: return book
			book.googleBooksId = request.getString("id")
		}
		val bookObj = request.getJSONObject("volumeInfo")
		if(book.subtitle == null)
			book.subtitle = bookObj.optString("subtitle")
		if (book.publisher == null)
			book.publisher = bookObj.optString("publisher")
		return book.push()
	}

	fun searchBook(isbn: Isbn): Book? {
		val url = "$URL?q=isbn:${isbn}"
		val request = Util.httpRequest(url)?.`object`?.optJSONArray("items")?.optJSONObject(0) ?: return null
		val googleBooksId = request.getString("id")
		val bookObj = request.getJSONObject("volumeInfo")
		val title = bookObj.getString("title")
		val subtitle = bookObj.optString("subtitle")
		val publisher = bookObj.optString("publisher")
		return Book(
			isbn = isbn,
			title = title,
			subtitle = subtitle,
			publisher = publisher,
			googleBooksId = googleBooksId
		).add()
	}
}