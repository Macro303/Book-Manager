package macro.library.external

import macro.library.Util
import macro.library.book.Book
import macro.library.book.Isbn
import macro.library.config.Config.Companion.CONFIG
import org.apache.logging.log4j.LogManager
import org.json.XML

/**
 * Created by Macro303 on 2019-Nov-13
 */
object Goodreads {
	private val LOGGER = LogManager.getLogger(Goodreads::class.java)
	private val URL = "https://www.goodreads.com/book"

	fun searchBook(isbn: Isbn): Book? {
		val bookId = searchBookId(isbn) ?: return null
		val url = "$URL/show/$bookId.json?key=${CONFIG.goodreads}"
		val request = XML.toJSONObject(Util.httpStrRequest(url) ?: return null)
		LOGGER.info("Request: $request")
		return null
	}

	private fun searchBookId(isbn: Isbn): Int?{
		val url = "$URL/isbn_to_id/$isbn?key=${CONFIG.goodreads}"
		return Util.httpStrRequest(url)?.toIntOrNull()
	}
}