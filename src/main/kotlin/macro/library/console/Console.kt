package macro.library.console

import macro.library.Util
import macro.library.book.Book
import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-02
 */
object Console {
	private val LOGGER = LogManager.getLogger(Console::class.java)

	init {
		colourTest()
	}

	private fun colourTest() {
		Colour.values().forEach {
			LOGGER.debug("${it.name} => ${it.ansicode}Test Message${Colour.RESET.ansicode}")
		}
	}

	internal fun displayHeader(text: String) {
		colourConsole(text = "=".repeat(text.length + 4), colour = Colour.BLUE)
		displaySubHeader(text = text)
		colourConsole(text = "=".repeat(text.length + 4), colour = Colour.BLUE)
	}

	internal fun displaySubHeader(text: String) {
		colourConsole(text = "  $text  ", colour = Colour.BLUE)
	}

	internal fun displayMenu(header: String, options: Set<String>, exit: String? = "Back"): Int {
		displayHeader(text = header)
		if (options.isEmpty()) return 0
		val padCount = options.size.toString().length
		options.forEachIndexed { index, option ->
			displayItemValue(item = (index + 1).toString().padStart(padCount), value = option)
		}
		if (exit != null) displayItemValue(item = "0".padStart(padCount), value = exit)
		return displayPrompt(text = "Option").toIntOrNull() ?: 0
	}

	internal fun displayPrompt(text: String): String {
		return Reader.readConsole(text = text).trim()
	}

	internal fun displayAgreement(text: String): Boolean {
		val input = displayPrompt(text = "$text (Y/N)")
		return input.equals("y", ignoreCase = true)
	}

	internal fun displayItemValue(item: String, value: Any?) {
		colourConsole(text = "$item: ", colour = Colour.BLUE, newLine = false)
		colourConsole(text = value.toString())
	}

	internal fun display(text: String, colour: Colour = Colour.WHITE) {
		colourConsole(text = text, colour = colour)
	}

	internal fun displayTable(books: List<Book>) {
		var isbnSize = Util.displayISBN(books.maxBy { Util.displayISBN(it.isbn).length }?.isbn).length
		if (isbnSize < 4)
			isbnSize = 4
		val nameSize = books.maxBy { it.name.length }?.name?.length ?: 4
		val authorSize = books.maxBy { it.author?.length ?: 6 }?.author?.length ?: 6
		val seriesSize = books.maxBy { it.series?.length ?: 6 }?.series?.length ?: 6
		val seriesNumSize = books.maxBy { it.seriesNum?.toString()?.length ?: 8 }?.seriesNum?.toString()?.length ?: 8
		val formatSize = books.maxBy { it.format.name.length }?.format?.name?.length ?: 6
		val titles = listOf(
			Pair("ISBN", isbnSize),
			Pair("Name", nameSize),
			Pair("Author", authorSize),
			Pair("Series", seriesSize),
			Pair("Series #", seriesNumSize),
			Pair("Format", formatSize)
		)
		colourConsole(text = titles.joinToString(" | ") { it.first.padEnd(it.second) }, colour = Colour.BLUE)
		colourConsole(text = titles.joinToString(" | ") { "-".repeat(it.second) }, colour = Colour.BLUE)
		books.sorted().forEach {
			colourConsole(
				text = (Util.displayISBN(it.isbn)).padStart(isbnSize) + " | " +
						it.name.padEnd(nameSize) + " | " +
						(it.author ?: "").padEnd(authorSize) + " | " +
						(it.series ?: "").padEnd(seriesSize) + " | " +
						(it.seriesNum?.toString() ?: "").padStart(seriesNumSize) + " | " +
						it.format.display.padEnd(formatSize),
				colour = Colour.WHITE
			)
		}
	}

	private fun colourConsole(text: String?, colour: Colour = Colour.WHITE, newLine: Boolean = true) {
		if (newLine)
			println("${colour.ansicode}$text${Colour.RESET.ansicode}")
		else
			print("${colour.ansicode}$text${Colour.RESET.ansicode}")
	}
}