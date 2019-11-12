package macro.library.book

import macro.library.ISendable
import macro.library.database.BookTable
import macro.library.database.WishlistTable

/**
 * Created by Macro303 on 2019-Oct-31
 */
data class WishlistBook(val bookId: Isbn, var count: Int = 1) : Comparable<WishlistBook>, ISendable {

	fun getBook(): Book = BookTable.selectUnique(bookId) ?: throw NullPointerException("Unable to find related book")

	fun add(): WishlistBook {
		WishlistTable.insert(this)
		return this
	}

	fun push(): WishlistBook {
		WishlistTable.update(this)
		return this
	}

	fun remove() {
		WishlistTable.delete(bookId)
	}

	override fun compareTo(other: WishlistBook): Int = comparator.compare(this, other)
	override fun toJson(full: Boolean, showUnique: Boolean): Map<String, Any?> {
		val output = mutableMapOf<String, Any?>(
			"Book" to getBook().toJson(full = full, showUnique = true),
			"Count" to count
		)
		return output.toSortedMap()
	}

	companion object {
		private val comparator = compareBy(WishlistBook::getBook)
	}
}