package github.macro.book

import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-30
 */
class Isbn(var isbnString: String) : Comparable<Isbn> {

	init {
		this.isbnString = isbnString.replace("-", "")
	}

	override fun compareTo(other: Isbn): Int = isbnString.compareTo(other.isbnString, ignoreCase = true)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Isbn) return false

		if (isbnString != other.isbnString) return false

		return true
	}

	override fun hashCode(): Int {
		return isbnString.hashCode()
	}

	override fun toString(): String {
		return isbnString
	}

	companion object {
		private val LOGGER = LogManager.getLogger(Isbn::class.java)

		fun of(number: String?): Isbn? {
			number ?: return null
			if (!isValid(number))
				return null

			return if (number.replace("-", "").length == 10) Isbn(toIsbn13(number)) else Isbn(number)
		}

		private fun isValid(numberSequence: String): Boolean {
			val normalizedSequence = numberSequence.replace("-".toRegex(), "")
			if (normalizedSequence.length == 13)
				return isValidAsIsbn13(normalizedSequence)
			return false
		}

		private fun toIsbn13(isbn10: String): String {
			val digits = "978${isbn10.substring(0, isbn10.length - 1)}"

			val checkDigit = isbn13CheckDigit(digits)
			return digits + checkDigit
		}

		private fun isValidAsIsbn13(number: String): Boolean {
			return number[12].toString().toInt() == isbn13CheckDigit(number.substring(0, 12))
		}

		private fun isbn13CheckDigit(digits: String): Int {
			val weights = intArrayOf(1, 3)
			var sum = 0
			digits.forEachIndexed { index, digit ->
				sum += digit.toString().toInt() * weights[index % 2]
			}
			return if (sum % 10 == 0) 0 else 10 - sum % 10
		}
	}
}