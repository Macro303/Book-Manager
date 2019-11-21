package macro.library.book

import macro.library.ISendable
import macro.library.database.BookTable
import macro.library.database.CollectionTable

/**
 * Created by Macro303 on 2019-Oct-31
 */
data class CollectionBook(val bookId: Isbn, var count: Int = 1) : Comparable<CollectionBook>, ISendable {

	fun getBook(): Book = BookTable.selectUnique(bookId) ?: throw NullPointerException("Unable to find related book")

	fun add(): CollectionBook {
		CollectionTable.insert(this)
		return this
	}

	fun push(): CollectionBook {
		CollectionTable.update(this)
		return this
	}

	fun remove() {
		CollectionTable.delete(bookId)
	}

	override fun compareTo(other: CollectionBook): Int = comparator.compare(this, other)
	override fun toJson(full: Boolean): Map<String, Any?> {
		val output = mutableMapOf<String, Any?>(
			"Book" to getBook().toJson(),
			"Count" to count
		)
		return output.toSortedMap()
	}

	companion object {
		private val comparator = compareBy(CollectionBook::getBook)
	}
}