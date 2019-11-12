package macro.library.console

import macro.library.book.Book
import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-30
 */
object Console {
	private val LOGGER = LogManager.getLogger(Console::class.java)

	fun displayHeader(text: String) {
		colourConsole("=".repeat(text.length + 4), Colour.BLUE)
		displaySubHeader(text)
		colourConsole("=".repeat(text.length + 4), Colour.BLUE)
	}

	fun displaySubHeader(text: String) {
		colourConsole("  $text  ", Colour.BLUE)
	}

	fun displayMenu(header: String, options: List<String>, exit: String? = "Back"): Int {
		displayHeader(header)
		if (options.isEmpty())
			return 0
		val padCount = options.size.toString().length
		for (i in options.indices)
			displayItemValue((i + 1).toString().padStart(padCount), options[i])
		if (exit != null)
			displayItemValue("0".padStart(padCount), exit)
		return displayPrompt("Option").toIntOrNull() ?: 0
	}

	fun displayAgreement(text: String): Boolean {
		val input = displayPrompt("$text (Y/N)")
		return input.equals("y", ignoreCase = true)
	}

	fun displayPrompt(text: String): String {
		return Reader.readConsole(text).trim()
	}

	fun display(text: String?, colour: Colour = Colour.WHITE) {
		colourConsole(text, colour)
	}

	private fun colourConsole(text: String?, colour: Colour, newLine: Boolean = true) {
		if (newLine)
			println("$colour$text${Colour.RESET}")
		else
			print("$colour$text${Colour.RESET}")
	}

	fun displayTable(books: List<Book>) {
		var isbnSize = books.maxBy { it.isbn.toString().length }?.isbn?.toString()?.length ?: 4
		if (isbnSize < 4)
			isbnSize = 4
		var titleSize = books.maxBy { it.title.length }?.title?.length ?: 5
		if (titleSize < 5)
			titleSize = 5
		var subtitleSize = books.maxBy { it.subtitle?.length ?: 8 }?.subtitle?.length ?: 8
		if (subtitleSize < 8)
			subtitleSize = 8
		var authorSize = books.maxBy {
			it.authors.joinToString(separator = "; ").length
		}?.authors?.joinToString(separator = "; ")?.length ?: 6
		if (authorSize < 6)
			authorSize = 6
		var publisherSize = books.maxBy { it.publisher.length }?.publisher?.length ?: 9
		if (publisherSize < 9)
			publisherSize = 9
		var formatSize = books.maxBy { it.format.getDisplay().length }?.format?.getDisplay()?.length ?: 6
		if (formatSize < 6)
			formatSize = 6
		var titleOutput = "| "
		titleOutput += "ISBN".padStart(isbnSize) + " | "
		titleOutput += "Title".padEnd(titleSize) + " | "
		titleOutput += "Subtitle".padEnd(subtitleSize) + " | "
		titleOutput += "Author".padEnd(authorSize) + " | "
		titleOutput += "Publisher".padEnd(publisherSize) + " | "
		titleOutput += "Format".padEnd(formatSize) + " | "
		colourConsole(titleOutput, Colour.BLUE)
		var tableOutput = "| "
		tableOutput += "-".repeat(isbnSize) + " | "
		tableOutput += "-".repeat(titleSize) + " | "
		tableOutput += "-".repeat(subtitleSize) + " | "
		tableOutput += "-".repeat(authorSize) + " | "
		tableOutput += "-".repeat(publisherSize) + " | "
		tableOutput += "-".repeat(formatSize) + " | "
		colourConsole(tableOutput, Colour.BLUE)
		for (book in books.sorted()) {
			var bookOutput = "${Colour.BLUE}| ${Colour.WHITE}"
			bookOutput += book.isbn.toString().padStart(isbnSize) + Colour.BLUE + " | " + Colour.WHITE
			bookOutput += book.title.padEnd(titleSize) + Colour.BLUE + " | " + Colour.WHITE
			bookOutput += (book.subtitle ?: "").padEnd(subtitleSize) + Colour.BLUE + " | " + Colour.WHITE
			bookOutput += book.authors.joinToString(separator = "; ").padEnd(authorSize) + Colour.BLUE + " | " + Colour.WHITE
			bookOutput += book.publisher.padEnd(publisherSize) + Colour.BLUE + " | " + Colour.WHITE
			bookOutput += book.format.getDisplay().padEnd(formatSize) + Colour.BLUE + " | " + Colour.WHITE
			colourConsole(bookOutput, Colour.WHITE)
		}
	}

	fun displayEdit(title: String, current: String?): String? {
		displayItemValue(title, current)
		val input = displayPrompt("New Value (Blank to skip)")
		return if (input.isBlank()) null else input
	}

	fun displayItemValue(item: String, value: Any?) {
		colourConsole("$item: ", Colour.BLUE, false)
		colourConsole(value.toString(), Colour.WHITE)
	}
}