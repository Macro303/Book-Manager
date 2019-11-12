package macro.library.external

import macro.library.Util
import macro.library.book.Book
import macro.library.book.Isbn
import macro.library.book.author.Author
import macro.library.database.AuthorTable
import org.apache.logging.log4j.LogManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Created by Macro303 on 2019-Nov-12
 */
object OpenLibrary {
	private val LOGGER = LogManager.getLogger(OpenLibrary::class.java)
	private val URL = "http://openlibrary.org/api/books"

	fun searchBook(isbn: Isbn): Book? {
		val url = "$URL?bibkeys=ISBN:$isbn&format=json&jscmd=data"
		val request = Util.httpRequest(url) ?: return null
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
		val imageSmall = bookObj.getJSONObject("cover").getString("small")
		val imageMedium = bookObj.getJSONObject("cover").getString("medium")
		val imageLarge = bookObj.getJSONObject("cover").getString("large")
		searchAuthors(isbn)
		return Book(
			isbn = isbn,
			title = title,
			subtitle = subtitle,
			publisher = publisherStr.toString(),
			imageSmall = imageSmall,
			imageMedium = imageMedium,
			imageLarge = imageLarge
		)
	}

	fun searchAuthors(isbn: Isbn): List<Author> {
		val authorList = ArrayList<Author>()
		val url = "$URL?bibkeys=ISBN:$isbn&format=json&jscmd=data"
		val request = Util.httpRequest(url) ?: return authorList
		val response = request.getObject()
		val bookObj = response.optJSONObject("ISBN:$isbn") ?: return authorList
		var authors: JSONArray? = bookObj.optJSONArray("authors")
		if (authors == null)
			authors = JSONArray()
		for (authorName in authors) {
			val name = (authorName as JSONObject).getString("name")
			var author = Author.parseName(name)
			val found = AuthorTable.select(author.firstName, author.lastName)
			if (found == null)
				author = author.add()
			authorList.add(author)
		}
		return authorList
	}
}