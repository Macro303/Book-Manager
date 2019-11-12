package macro.library.book

import macro.library.ISendable
import macro.library.book.author.Author
import macro.library.database.BookAuthorTable
import macro.library.database.BookTable

/**
 * Created by Macro303 on 2019-Oct-30
 */
data class Book(
	val isbn: Isbn,
	var title: String,
	var subtitle: String?,
	var publisher: String,
	var format: Format = Format.PAPERBACK,
	var imageSmall: String? = null,
	var imageMedium: String? = null,
	var imageLarge: String? = null
) : Comparable<Book>, ISendable {

	val authors: List<Author> = BookAuthorTable.searchBook(isbn)

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

	override fun toJson(full: Boolean, showUnique: Boolean): Map<String, Any?> {
		val output = mutableMapOf<String, Any?>(
			"Title" to title,
			"Subtitle" to subtitle,
			"Format" to format.getDisplay(),
			"Images" to mutableMapOf<String, Any?>(
				"Small" to imageSmall,
				"Medium" to imageMedium,
				"Large" to imageLarge
			).toSortedMap()
		)
		if (full) {
			output["Authors"] = authors.map { it.toJson(full = false) }
			output["Publisher"] = publisher
		}
		if (showUnique)
			output["ISBN"] = isbn.toString()
		return output.toSortedMap()
	}

	companion object {
		private val comparator = compareBy(String.CASE_INSENSITIVE_ORDER, Book::title)
			.thenBy(nullsLast(), Book::subtitle)
			.thenBy(Book::format)
			.thenBy(Book::isbn)
	}
}