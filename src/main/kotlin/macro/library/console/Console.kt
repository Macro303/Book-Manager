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

	@JvmStatic
	internal fun displayHeader(text: String) {
		colourConsole(text = "=".repeat(text.length + 4), colour = Colour.BLUE)
		displaySubHeader(text = text)
		colourConsole(text = "=".repeat(text.length + 4), colour = Colour.BLUE)
	}

	@JvmStatic
	internal fun displaySubHeader(text: String) {
		colourConsole(text = "  $text  ", colour = Colour.BLUE)
	}

	@JvmStatic
	@JvmOverloads
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

	@JvmStatic
	@JvmOverloads
	internal fun displayMenu(header: String, options: Array<String>, exit: String? = "Back"): Int {
		return displayMenu(header, options.toHashSet(), exit)
	}

	@JvmStatic
	internal fun displayPrompt(text: String): String {
		return Reader.readConsole(text = text).trim()
	}

	@JvmStatic
	internal fun displayAgreement(text: String): Boolean {
		val input = displayPrompt(text = "$text (Y/N)")
		return input.equals("y", ignoreCase = true)
	}

	@JvmStatic
	internal fun displayItemValue(item: String, value: Any?) {
		colourConsole(text = "$item: ", colour = Colour.BLUE, newLine = false)
		colourConsole(text = value.toString())
	}

	@JvmStatic
	@JvmOverloads
	internal fun display(text: String, colour: Colour = Colour.WHITE) {
		colourConsole(text = text, colour = colour)
	}

	@JvmStatic
	internal fun displayTable(books: List<Book>) {
		var isbnSize = books.maxBy { it.isbn.toString().length }?.isbn.toString().length
		if (isbnSize < 4)
			isbnSize = 4
		var nameSize = books.maxBy { it.name.length }?.name?.length ?: 4
		if (nameSize < 4)
			nameSize = 4
		var authorSize = books.maxBy { it.author?.length ?: 6 }?.author?.length ?: 6
		if (authorSize < 6)
			authorSize = 6
		var seriesSize = books.maxBy { it.series?.length ?: 6 }?.series?.length ?: 6
		if (seriesSize < 6)
			seriesSize = 6
		var seriesNumSize = books.maxBy { it.seriesNum?.toString()?.length ?: 8 }?.seriesNum?.toString()?.length ?: 8
		if (seriesNumSize < 8)
			seriesNumSize = 8
		var formatSize = books.maxBy { it.format.name.length }?.format?.name?.length ?: 6
		if (formatSize < 6)
			formatSize = 6
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
				text = (it.isbn?.toString() ?: "").padStart(isbnSize) + " | " +
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