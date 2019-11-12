package macro.library.console


/**
 * Created by Macro303 on 2019-Oct-30
 */
enum class Colour(private val ansicode: String) {
	RESET("\u001B[0m"),
	BLACK("\u001B[30;1m"),
	RED("\u001B[31;1m"),
	GREEN("\u001B[32;1m"),
	YELLOW("\u001B[33;1m"),
	BLUE("\u001B[34;1m"),
	MAGENTA("\u001B[35;1m"),
	CYAN("\u001B[36;1m"),
	WHITE("\u001B[37;1m");

	override fun toString(): String {
		return ansicode
	}
}