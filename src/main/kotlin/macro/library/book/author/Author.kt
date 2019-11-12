package macro.library.book.author

import macro.library.ISendable
import macro.library.book.Book
import macro.library.database.AuthorTable
import macro.library.database.BookAuthorTable
import java.util.*

/**
 * Created by Macro303 on 2019-Oct-31
 */
data class Author(
	val uuid: UUID = UUID.randomUUID(),
	var firstName: String,
	var lastName: String,
	var middleNames: List<String>?
) : Comparable<Author>, ISendable {

	fun getBooks(): List<Book> = BookAuthorTable.searchAuthor(uuid)

	fun getDisplay(): String = "$lastName, $firstName${" " + middleNames?.joinToString(" ")}"

	fun add(): Author {
		AuthorTable.insert(this)
		return this
	}

	fun push(): Author {
		AuthorTable.update(this)
		return this
	}

	fun remove() {
		AuthorTable.delete(uuid)
	}

	override fun compareTo(other: Author): Int = comparator.compare(this, other)

	override fun toJson(full: Boolean, showUnique: Boolean): Map<String, Any?> {
		val output = mutableMapOf(
			"First Name" to firstName,
			"Last Name" to lastName,
			"Middle Names" to (middleNames ?: emptyList())
		)
		if (full)
			output["Books"] = getBooks().map { it.toJson(full = false) }
		if (showUnique)
			output["UUID"] = uuid.toString()
		return output.toSortedMap()
	}

	companion object {
		private val comparator = compareBy(String.CASE_INSENSITIVE_ORDER, Author::lastName)
			.thenBy(String.CASE_INSENSITIVE_ORDER, Author::firstName)

		fun parseName(name: String): Author {
			val nameParts = name.split(" ").toMutableList()
			val first = nameParts.removeAt(0).trim()
			val last = nameParts.removeAt(nameParts.size - 1).trim()
			var middleNames: List<String>? = nameParts
			if (nameParts.isNullOrEmpty())
				middleNames = null
			return Author(firstName = first, lastName = last, middleNames = middleNames)
		}
	}
}