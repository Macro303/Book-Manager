package macro.library.book

import macro.library.console.Console

/**
 * Created by Macro303 on 2019-Oct-30
 */
enum class Format {
	PAPERBACK,
	HARDCOVER,
	MASS_MEDIA_PAPERBACK;

	fun getDisplay(): String = name.toLowerCase().replace("_", " ").split(" ").joinToString(" ") { it.capitalize() }

	companion object {
		fun selection(): Format {
			val selection = Console.displayMenu("Format", values().map { it.getDisplay() }, null)
			return try {
				values()[selection - 1]
			} catch (oe: IndexOutOfBoundsException) {
				PAPERBACK
			}
		}

		fun parse(option: String): Format {
			return values().firstOrNull { option.equals(it.getDisplay(), ignoreCase = true) } ?: PAPERBACK
		}
	}
}