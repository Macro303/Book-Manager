package macro.library.book

/**
 * Created by Macro303 on 2019-Oct-30
 */
enum class Format {
	PAPERBACK,
	HARDCOVER,
	MASS_MEDIA_PAPERBACK;

	fun getDisplay(): String = name.toLowerCase().replace("_", " ").split(" ").joinToString(" ") { it.capitalize() }

	companion object {
		fun parse(option: String): Format {
			values().forEach {
				if (it.getDisplay().equals(option, ignoreCase = true))
					return it
			}
			return PAPERBACK
		}
	}
}