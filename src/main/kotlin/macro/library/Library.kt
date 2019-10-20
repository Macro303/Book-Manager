package macro.library

import macro.library.book.Book
import macro.library.book.BookTable
import macro.library.book.Format
import macro.library.console.Colour
import macro.library.console.Console
import org.apache.logging.log4j.LogManager

/**
 * Created by Macro303 on 2019-Oct-01
 */
object Library {
	private val LOGGER = LogManager.getLogger(Library::class.java)

	init {
		LOGGER.info("Initializing Book Manager")
		checkLogLevels()
	}

	private fun checkLogLevels() {
		LOGGER.trace("TRACE | is Visible")
		LOGGER.debug("DEBUG | is Visible")
		LOGGER.info("INFO  | is Visible")
		LOGGER.warn("WARN  | is Visible")
		LOGGER.error("ERROR | is Visible")
		LOGGER.fatal("FATAL | is Visible")
	}

	private fun mainMenu() {
		val options = setOf("List Books", "Add Book", "Import Books")
		val selection = Console.displayMenu(header = "Book Manager", options = options, exit = "Exit")
		when (selection) {
			0 -> return
			1 -> listBooks()
			2 -> addBook()
			3 -> LOGGER.info("Import Books")
		}
		mainMenu()
	}

	private fun addBook() {
		Console.displaySubHeader(text = "Add Book")
		val isbn = Console.displayPrompt(text = "ISBN").replace("-", "").toLongOrNull() ?: -1
		if(!validISBN(isbn = isbn)) {
			Console.display(text = "Invalid ISBN Number", colour = Colour.RED)
			return
		}
		val name = Console.displayPrompt(text = "Name")
		val author = Console.displayPrompt(text = "Author")
		val series = Console.displayPrompt(text = "Series")
		val seriesNum = Console.displayPrompt(text = "Series #").toIntOrNull()
		val options = Format.values()
		val selection = Console.displayMenu(header = "Format", options = options.map { it.display }.toSet(), exit = null)
		val format = options[selection - 1]
		BookTable.selectUnique(isbn = isbn) ?: Book(
			isbn = isbn,
			name = name,
			author = author,
			series = series,
			seriesNum = seriesNum,
			format = format
		).add()
	}

	private fun listBooks(){
		val books = BookTable.searchAll()
		Console.displayTable(books = books)
	}

	private fun validISBN(isbn: Long): Boolean{
		if (isbn.toString().length == 10)
			return validISBN10(isbn = isbn)
		if(isbn.toString().length == 13)
			return validISBN13(isbn = isbn)
		return false
	}

	private fun validISBN10(isbn: Long): Boolean{
		var sum = 0
		isbn.toString().reversed().forEachIndexed { index, c ->
			val num = c.toString().toInt()
			sum += (num * (index + 1))
		}
		return sum.rem(11) == 0
	}

	private fun validISBN13(isbn: Long): Boolean{
		var sum = 0
		isbn.toString().reversed().forEachIndexed { index, c ->
			val num = c.toString().toInt()
			sum += if ((index + 1).rem(2) == 1) num else num * 3
		}
		return sum.rem(10) == 0
	}

	@JvmStatic
	fun main(args: Array<String>) {
		mainMenu()
	}
}