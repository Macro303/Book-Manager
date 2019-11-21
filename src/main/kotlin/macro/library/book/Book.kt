package macro.library.book

import macro.library.ISendable
import macro.library.database.BookTable
import macro.library.external.GoogleBooks
import macro.library.external.OpenLibrary
import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-30
 */
data class Book(
	val isbn: Isbn,
	var title: String,
	var subtitle: String?,
	var publisher: String?,
	var format: Format = Format.PAPERBACK,
	var openLibraryId: String? = null,
	var googleBooksId: String? = null,
	var goodreadsId: String? = null
) : Comparable<Book>, ISendable {

	fun add(): Book {
		BookTable.insert(this)
		return this
	}

	fun push(): Book {
		BookTable.update(this)
		return this
	}

	fun remove() {
		BookTable.delete(isbn)
	}

	override fun compareTo(other: Book): Int = comparator.compare(this, other)

	override fun toJson(full: Boolean): Map<String, Any?> {
		val output = mutableMapOf<String, Any?>(
			"ISBN" to isbn.toString(),
			"Title" to title,
			"Subtitle" to subtitle,
			"Format" to format.getDisplay(),
			"Images" to mutableMapOf<String, Any?>(
				"Small" to (if (openLibraryId != null) "https://covers.openlibrary.org/b/OLID/$openLibraryId-S.jpg" else if (googleBooksId != null) "http://books.google.com/books/content?id=$googleBooksId&printsec=frontcover&img=1&zoom=1" else null),
				"Medium" to (if (openLibraryId != null) "https://covers.openlibrary.org/b/OLID/$openLibraryId-M.jpg" else if (googleBooksId != null) "http://books.google.com/books/content?id=$googleBooksId&printsec=frontcover&img=1&zoom=5" else null),
				"Large" to (if (openLibraryId != null) "https://covers.openlibrary.org/b/OLID/$openLibraryId-L.jpg" else if (googleBooksId != null) "http://books.google.com/books/content?id=$googleBooksId&printsec=frontcover&img=1&zoom=10" else null)
			).toSortedMap()
		)
		if (full) {
			output["Publisher"] = publisher
			output["External IDs"] = mutableMapOf<String, Any?>(
				"Open Library" to openLibraryId,
				"Google Books" to googleBooksId,
				"Goodreads" to goodreadsId
			).toSortedMap()
		}
		return output.toSortedMap()
	}

	companion object {
		private val LOGGER = LogManager.getLogger(Book::class.java)
		private val comparator = compareBy(String.CASE_INSENSITIVE_ORDER, Book::title)
			.thenBy(nullsLast(), Book::subtitle)
			.thenBy(Book::format)
			.thenBy(Book::isbn)

		fun create(isbn: Isbn, format: Format = Format.PAPERBACK): Book? {
			var book = BookTable.selectUnique(isbn)
			if (book == null) {
				LOGGER.info("Book not found in DB")
				book = OpenLibrary.searchBook(isbn)
				if (book == null) {
					LOGGER.info("Book not found in Open Library")
					book = GoogleBooks.searchBook(isbn)
					if (book == null) {
						LOGGER.info("Book not found in Google Books")
						return null
					}
				} else
					book = GoogleBooks.selectBook(book)
			} else {
				book = OpenLibrary.selectBook(book)
				book = GoogleBooks.selectBook(book)
			}
			book.format = format
			return book.push()
		}
	}
}